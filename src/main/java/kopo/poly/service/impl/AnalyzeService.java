package kopo.poly.service.impl;

import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.dto.RecipeDTO;
import kopo.poly.persistance.mongodb.IAnalysisResultMapper;
import kopo.poly.service.IAnalyzeService;
import kopo.poly.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService implements IAnalyzeService {

    private final S3Util s3Util;
    private final WebClient webClient;
    private final IAnalysisResultMapper analysisResultMapper;
    // application.yml: fastapi.base-url=http://localhost:8000
    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;

    // FastAPI 분석 응답 대기 타임아웃 (VLM 처리 시간 고려 90초)
    private static final Duration ANALYZE_TIMEOUT = Duration.ofSeconds(90);

    @Override
    public AnalysisResultDTO analyzeImage(String savedFilename) {

        log.info("{}.analyzeImage Start! savedFilename={}", this.getClass().getName(), savedFilename);

        // ── STEP 1: 다운로드용 Presigned URL 발급 (S3Util 재사용) ──
        String downloadUrl = s3Util.generatePresignedUrlToDownload(savedFilename);
        log.info("Presigned download URL 발급 완료: {}", savedFilename);

        // ── STEP 2: FastAPI 분석 요청 ──────────────────────────────
        // 요청 바디: { "download_url": "...", "s3_key": "..." }
        Map<String, String> requestBody = Map.of(
                "download_url", downloadUrl,
                "s3_key",       savedFilename
        );

        AnalysisResultDTO result;
        try {
            result = webClient.post()
                    .uri(fastapiBaseUrl + "/api/v1/fridge/analyze")
                    .bodyValue(requestBody)
                    .retrieve()
                    // 4xx — FastAPI 비즈니스 오류를 AnalysisResultDTO로 매핑
                    .onStatus(HttpStatus.BAD_REQUEST::equals, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new IllegalArgumentException("FastAPI 요청 오류: " + body))))
                    // 5xx — FastAPI 내부 오류
                    .onStatus(status -> status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("FastAPI 서버 오류: " + body))))
                    .bodyToMono(AnalysisResultDTO.class)
                    .timeout(ANALYZE_TIMEOUT)
                    .block();  // 동기 방식 — 타임아웃 90초

            if (result != null && result.success()) {
                // FOOD_BEFORE / FOOD_AFTER 연결을 위한 scanId 생성
                String scanId = UUID.randomUUID().toString();
                result = new AnalysisResultDTO(
                        result.success(), result.detectedCount(),
                        result.ingredients(), result.errorMessage(), scanId);
                log.info("FastAPI 응답 수신 detectedCount={} ingredientCount={} scanId={}",
                        result.detectedCount(),
                        result.ingredients() != null ? result.ingredients().size() : 0,
                        scanId);
            } else {
                log.warn("FastAPI 분석 실패 응답 errorMessage={}",
                        result != null ? result.errorMessage() : "null");
            }

        } catch (WebClientResponseException e) {
            log.error("FastAPI 호출 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            result = new AnalysisResultDTO(false, 0, null, "FastAPI 통신 오류: " + e.getMessage(), null);

        } catch (Exception e) {
            log.error("FastAPI 호출 중 예외 발생: {}", e.getMessage(), e);
            result = new AnalysisResultDTO(false, 0, null, "분석 요청 실패: " + e.getMessage(), null);
        }

        log.info("{}.analyzeImage End! success={}", this.getClass().getName(),
                result != null && result.success());

        return result;
    }

    @Override
    public int saveFoodResult(AnalysisResultDTO pDTO) throws Exception {

        log.info("{}.saveFoodResult Start! scanId={}", this.getClass().getName(), pDTO.scanId());

        int res = analysisResultMapper.insertDate(pDTO, "FOOD_BEFORE");

        log.info("{}.saveFoodResult End!", this.getClass().getName());

        return res;
    }

    @Override
    public int saveFoodAfterResult(AnalysisResultDTO pDTO) throws Exception {

        log.info("{}.saveFoodAfterResult Start! scanId={}", this.getClass().getName(), pDTO.scanId());

        int res = analysisResultMapper.insertDate(pDTO, "FOOD_AFTER");

        log.info("{}.saveFoodAfterResult End!", this.getClass().getName());

        return res;
    }

    @Override
    public List<RecipeDTO> analyzeRecipes(String scanId) throws Exception {

        log.info("{}.analyzeRecipes Start! scanId={}", this.getClass().getName(), scanId);

        // STEP 1: FOOD_AFTER에서 해당 scanId의 식재료명 목록 조회
        List<String> ingredients = analysisResultMapper.getFoodResult("FOOD_AFTER", scanId);
        log.info("식재료 목록 조회 완료 | scanId={} count={}", scanId, ingredients.size());

        if (ingredients.isEmpty()) {
            log.warn("식재료 목록이 비어있음 — FastAPI 호출 생략 | scanId={}", scanId);
            return List.of();
        }

        // STEP 2: FastAPI 레시피 분석 요청
        Map<String, Object> requestBody = Map.of(
                "ingredients", ingredients,
                "scanId",      scanId
        );

        List<RecipeDTO> rList;
        try {
            rList = webClient.post()
                    .uri(fastapiBaseUrl + "/api/v1/recipes/analyze")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(HttpStatus.BAD_REQUEST::equals, response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new IllegalArgumentException("FastAPI 레시피 요청 오류: " + body))))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("FastAPI 레시피 서버 오류: " + body))))
                    .bodyToMono(new ParameterizedTypeReference<List<RecipeDTO>>() {})
                    .timeout(ANALYZE_TIMEOUT)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("FastAPI 레시피 호출 실패 | status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("레시피 분석 요청 실패: " + e.getMessage(), e);
        }

        List<RecipeDTO> result = rList != null ? rList : List.of();
        log.info("{}.analyzeRecipes End! count={}", this.getClass().getName(), result.size());

        return result;
    }
}
