package kopo.poly.service.impl;

import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.dto.RecipeDTO;
import kopo.poly.dto.VideoSummaryDTO;
import kopo.poly.persistance.mongodb.IAnalysisResultMapper;
import kopo.poly.repository.RecipeRepository;
import kopo.poly.repository.entity.RecipeEntity;
import kopo.poly.service.IAnalyzeService;
import kopo.poly.util.S3Util;
import lombok.NonNull;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService implements IAnalyzeService {

    private final S3Util s3Util;
    private final WebClient webClient;
    private final IAnalysisResultMapper analysisResultMapper;
    private final RecipeRepository recipeRepository;
    // application.yml: fastapi.base-url=http://localhost:8000
    @Value("${fastapi.base-url}")
    private String fastapiBaseUrl;

    @Value("${youtube.data.base-url}")
    private String youtubeDataBaseUrl;

    @Value("${youtube.data.api-key}")
    private String youtubeDataApiKey;

    /** YouTube video ID 추출 정규식 — watch?v=, youtu.be/, /embed/ 형식 모두 지원 */
    private static final Pattern YT_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/)([^\"&?/\\s]{11})"
    );

    /** YouTube Data API 호출 타임아웃 — 빠른 메타데이터 조회이므로 5초 충분 */
    private static final Duration YOUTUBE_API_TIMEOUT = Duration.ofSeconds(5);

    // FastAPI 분석 응답 대기 타임아웃
    // - 이미지 분석(VLM)       : ~30초
    // - 레시피 분석(자막 기반)  : Agent(35s) + (yt-dlp+Gemini텍스트)×3(~10s×3) = ~65s → 여유 120초
    // - 영상 요약(Gemini 직접) : 단일 영상 Gemini 분석 → 여유 90초
    private static final Duration ANALYZE_TIMEOUT       = Duration.ofSeconds(120);
    private static final Duration VIDEO_SUMMARY_TIMEOUT = Duration.ofSeconds(90);

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
                result = AnalysisResultDTO.builder()
                        .success(result.success())
                        .detectedCount(result.detectedCount())
                        .ingredients(result.ingredients())
                        .errorMessage(result.errorMessage())
                        .scanId(scanId)
                        .build();
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
            result = AnalysisResultDTO.builder()
                    .success(false)
                    .errorMessage("FastAPI 통신 오류: " + e.getMessage())
                    .build();

        } catch (Exception e) {
            log.error("FastAPI 호출 중 예외 발생: {}", e.getMessage(), e);
            result = AnalysisResultDTO.builder()
                    .success(false)
                    .errorMessage("분석 요청 실패: " + e.getMessage())
                    .build();
        }

        log.info("{}.analyzeImage End! success={}", this.getClass().getName(),
                result != null && result.success());

        return result;
    }

    @Override
    public int saveFoodResult(@NonNull AnalysisResultDTO pDTO) throws Exception {

        log.info("{}.saveFoodResult Start! scanId={}", this.getClass().getName(), pDTO.scanId());

        int res = analysisResultMapper.insertDate(pDTO, "FOOD_BEFORE");

        log.info("{}.saveFoodResult End!", this.getClass().getName());

        return res;
    }

    @Override
    public int saveFoodAfterResult(@NonNull AnalysisResultDTO pDTO) throws Exception {

        log.info("{}.saveFoodAfterResult Start! scanId={}", this.getClass().getName(), pDTO.scanId());

        int res = analysisResultMapper.insertDate(pDTO, "FOOD_AFTER");

        log.info("{}.saveFoodAfterResult End!", this.getClass().getName());

        return res;
    }

    @Override
    public List<RecipeDTO> analyzeRecipes(@NonNull String scanId, @NonNull Integer userId) throws Exception {

        log.info("{}.analyzeRecipes Start! scanId={} userId={}", this.getClass().getName(), scanId, userId);

        // STEP 1: FOOD_AFTER에서 scanId + userId 소유권 검증 후 식재료명 목록 조회
        List<String> ingredients = analysisResultMapper.getFoodResult("FOOD_AFTER", scanId, userId);
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

        // ── STEP 3: YouTube 임베딩 허용 여부 사전 검증 ───────────────────────────
        // status.embeddable=false 인 영상은 youtube_url / youtube_url_thumbnail 을 null 로 교체
        // API 호출 실패(쿼터 초과 등) 시 원본 목록 유지 (fail-open)
        if (!result.isEmpty()) {
            result = filterNonEmbeddableYoutubeUrls(result);
        }

        // ── STEP 4: MariaDB INSERT ────────────────────────────────────────────
        if (!result.isEmpty()) {
            List<RecipeEntity> entities = result.stream()
                    .map(dto -> RecipeEntity.builder()
                            .scanId(dto.scanId() != null ? dto.scanId() : scanId)
                            .title(dto.title())
                            .content(dto.content())
                            .difficulty(dto.difficulty())
                            .cookingTime(dto.cooking_time())
                            .calories(dto.calories())
                            .youtubeUrl(dto.youtube_url())
                            .youtubeUrlThumbnail(dto.youtube_url_thumbnail())
                            .ingredients(dto.ingredients() != null ? dto.ingredients() : List.of())
                            .additionalIngredients(dto.additional_ingredients() != null ? dto.additional_ingredients() : List.of())
                            .recipeVideoSummary(null)   // 영상 요약은 getVideoSummary 호출 시 UPDATE
                            .build())
                    .toList();
            recipeRepository.saveAll(entities);
            log.info("레시피 MariaDB INSERT 완료 | count={} | scanId={}", entities.size(), scanId);
        }

        log.info("{}.analyzeRecipes End! count={}", this.getClass().getName(), result.size());

        return result;
    }

    @Override
    public List<VideoSummaryDTO> getVideoSummary(String youtubeUrl, String scanId, Integer userId) throws Exception {

        log.info("{}.getVideoSummary Start! scanId={} userId={} youtubeUrl={}", this.getClass().getName(), scanId, userId, youtubeUrl);

        Map<String, String> requestBody = Map.of(
                "youtube_url", youtubeUrl,
                "scan_id",     scanId
        );

        List<VideoSummaryDTO> steps;
        try {
            steps = webClient.post()
                    .uri(fastapiBaseUrl + "/api/v1/recipes/video-summary")
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new IllegalArgumentException("FastAPI 영상 요약 요청 오류: " + body))))
                    .onStatus(status -> status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("FastAPI 영상 요약 서버 오류: " + body))))
                    .bodyToMono(new ParameterizedTypeReference<List<VideoSummaryDTO>>() {})
                    .timeout(VIDEO_SUMMARY_TIMEOUT)
                    .block();

        } catch (Exception e) {
            log.error("FastAPI 영상 요약 호출 실패 | scanId={} error={}", scanId, e.getMessage());
            throw e;
        }

        List<VideoSummaryDTO> result = steps != null ? steps : List.of();

        // ── MariaDB UPDATE: 영상 요약 단계 + USER_ID 저장 ───────────────────
        if (!result.isEmpty()) {
            recipeRepository.findByScanIdAndYoutubeUrl(scanId, youtubeUrl)
                    .ifPresentOrElse(entity -> {
                        entity.updateVideoSummary(result, userId);
                        recipeRepository.save(entity);
                        log.info("영상 요약 + userId MariaDB UPDATE 완료 | scanId={} userId={} steps={}",
                                scanId, userId, result.size());
                    }, () -> log.warn("영상 요약 저장 대상 레시피 없음 | scanId={} youtubeUrl={}", scanId, youtubeUrl));
        }

        log.info("{}.getVideoSummary End! scanId={} steps={}", this.getClass().getName(), scanId, result.size());

        return result;
    }

    // ── YouTube 임베딩 필터 ───────────────────────────────────────────────────

    /**
     * YouTube Data API v3 {@code videos?part=status} 를 배치 호출하여
     * {@code status.embeddable=false} 인 레시피의 {@code youtube_url} / {@code youtube_url_thumbnail} 을 null 로 교체합니다.
     *
     * <ul>
     *   <li>모든 영상 ID를 한 번의 API 호출로 처리 (쿼터 절약)</li>
     *   <li>API 호출 실패(쿼터 초과·네트워크 오류) 시 원본 목록을 그대로 반환 (fail-open)</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    private List<RecipeDTO> filterNonEmbeddableYoutubeUrls(List<RecipeDTO> recipes) {

        // ── 1. 유효한 video ID 수집 ───────────────────────────────────────────
        List<String> videoIds = recipes.stream()
                .map(r -> extractYoutubeVideoId(r.youtube_url()))
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (videoIds.isEmpty()) {
            log.info("filterNonEmbeddableYoutubeUrls: youtube_url 없음 — 스킵");
            return recipes;
        }

        log.info("filterNonEmbeddableYoutubeUrls: YouTube API 배치 호출 | videoIds={}", videoIds);

        // ── 2. YouTube Data API v3 배치 호출 ─────────────────────────────────
        Set<String> embeddableIds = new HashSet<>();
        try {
            String ids = String.join(",", videoIds);
            Map<String, Object> response = webClient.get()
                    .uri(youtubeDataBaseUrl + "/videos?part=status&id={ids}&key={key}", ids, youtubeDataApiKey)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(YOUTUBE_API_TIMEOUT)
                    .block();

            if (response != null && response.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
                for (Map<String, Object> item : items) {
                    String id     = (String) item.get("id");
                    Map<String, Object> status = (Map<String, Object>) item.get("status");
                    if (id != null && status != null && Boolean.TRUE.equals(status.get("embeddable"))) {
                        embeddableIds.add(id);
                    }
                }
            }
            log.info("filterNonEmbeddableYoutubeUrls: 임베딩 허용 videoIds={}", embeddableIds);

        } catch (Exception e) {
            // API 호출 실패 → fail-open: 원본 목록 반환
            log.warn("filterNonEmbeddableYoutubeUrls: YouTube API 호출 실패 — 필터 생략 | error={}", e.getMessage());
            return recipes;
        }

        // ── 3. 비임베딩 영상 URL null 처리 ───────────────────────────────────
        return recipes.stream()
                .map(dto -> {
                    String videoId = extractYoutubeVideoId(dto.youtube_url());
                    if (videoId != null && !embeddableIds.contains(videoId)) {
                        log.warn("filterNonEmbeddableYoutubeUrls: 임베딩 차단 — url null 처리 | videoId={} title={}", videoId, dto.title());
                        return RecipeDTO.builder()
                                .recipeId(dto.recipeId())
                                .ingredients(dto.ingredients())
                                .title(dto.title())
                                .content(dto.content())
                                .difficulty(dto.difficulty())
                                .cooking_time(dto.cooking_time())
                                .calories(dto.calories())
                                .youtube_url(null)               // ← 임베딩 차단 → null
                                .youtube_url_thumbnail(null)     // ← 썸네일도 함께 제거
                                .scanId(dto.scanId())
                                .additional_ingredients(dto.additional_ingredients())
                                .recipe_video_summary(dto.recipe_video_summary())
                                .build();
                    }
                    return dto;
                })
                .toList();
    }

    /**
     * YouTube URL 에서 11자리 video ID 를 추출합니다.
     *
     * <p>지원 형식:</p>
     * <ul>
     *   <li>{@code https://www.youtube.com/watch?v=VIDEO_ID}</li>
     *   <li>{@code https://youtu.be/VIDEO_ID}</li>
     *   <li>{@code https://www.youtube.com/embed/VIDEO_ID}</li>
     * </ul>
     *
     * @return video ID (11자리) 또는 {@code null}
     */
    private String extractYoutubeVideoId(String url) {
        if (url == null || url.isBlank()) return null;
        Matcher matcher = YT_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
