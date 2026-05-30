package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.*;
import kopo.poly.service.IAnalyzeService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalyzeController {

    private final IAnalyzeService analyzeService;

    /**
     * 냉장고 이미지 분석 요청
     *
     * React → POST /api/analyze?filename=fridge-images/uuid.jpg
     * Spring → FastAPI (Presigned Download URL + s3Key 전달)
     * FastAPI → Roboflow → 크롭 → 그리드 → Gemini VLM → 결과 반환
     */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResultDTO> analyzeImage(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        log.info("{}.analyzeImage Start!", this.getClass().getName());

        Integer userId       = Integer.parseInt(jwt.getSubject());
        String savedFilename = CmmUtil.nvl(request.getParameter("filename"));

        log.info("분석 요청 | userId={} filename={}", userId, savedFilename);

        if (savedFilename.isEmpty()) {
            log.warn("filename 파라미터가 비어있음");
            return ResponseEntity.badRequest()
                    .body(AnalysisResultDTO.builder()
                            .success(false)
                            .errorMessage("filename 파라미터가 필요합니다.")
                            .build());
        }

        AnalysisResultDTO rDTO = analyzeService.analyzeImage(savedFilename);

        if (rDTO != null && rDTO.success()) {
            // userId 를 JWT 에서 강제 주입 — 클라이언트 전달값 신뢰 X
            rDTO = AnalysisResultDTO.builder()
                    .success(rDTO.success())
                    .detectedCount(rDTO.detectedCount())
                    .ingredients(rDTO.ingredients())
                    .errorMessage(rDTO.errorMessage())
                    .scanId(rDTO.scanId())
                    .userId(userId)
                    .build();
            try {
                int saveRes = analyzeService.saveFoodResult(rDTO);
                log.info("MongoDB 저장 완료 | userId={} result={}", userId, saveRes);
            } catch (Exception e) {
                log.error("MongoDB 저장 실패: {}", e.getMessage(), e);
            }
        }

        log.info("{}.analyzeImage End!", this.getClass().getName());

        return ResponseEntity.ok(rDTO);
    }

    /**
     * 사용자 수정 결과 저장
     *
     * React → POST /api/analyze/reviewed (body: 수정된 AnalysisResultDTO, scanId 포함)
     * Spring → MongoDB FOOD_AFTER 컬렉션 저장
     */
    @PostMapping("/analyze/reviewed")
    public ResponseEntity<MsgDTO> saveReviewedResult(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AnalysisResultDTO pDTO) {

        // 클라이언트가 body 에 전달한 userId 는 무시 — JWT sub 로 강제 덮어씀
        Integer userId = Integer.parseInt(jwt.getSubject());

        log.info("{}.saveAfterResult Start! scanId={} userId={}", this.getClass().getName(), pDTO.scanId(), userId);

        // userId 를 JWT 에서 주입한 DTO 로 재구성
        AnalysisResultDTO verifiedDTO = AnalysisResultDTO.builder()
                .success(pDTO.success())
                .detectedCount(pDTO.detectedCount())
                .ingredients(pDTO.ingredients())
                .errorMessage(pDTO.errorMessage())
                .scanId(pDTO.scanId())
                .userId(userId)
                .build();

        MsgDTO rDTO;
        try {
            int res = analyzeService.saveFoodAfterResult(verifiedDTO);
            rDTO = new MsgDTO(res, "저장 완료");
        } catch (Exception e) {
            log.error("FOOD_AFTER 저장 실패: {}", e.getMessage(), e);
            rDTO = new MsgDTO(0, "저장 실패: " + e.getMessage());
        }

        log.info("{}.saveAfterResult End!", this.getClass().getName());

        return ResponseEntity.ok(rDTO);
    }

    /**
     * 레시피 분석 요청
     *
     * React → POST /api/analyze/recipe?scanId=xxx
     * Spring → FOOD_AFTER 식재료 조회 → FastAPI 레시피 분석 → List<RecipeDTO> 반환
     */
    @PostMapping("/analyze/recipe")
    public ResponseEntity<List<RecipeDTO>> analyzeRecipe(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        String  scanId = CmmUtil.nvl(request.getParameter("scanId"));

        log.info("{}.analyzeRecipe Start! scanId={} userId={}", this.getClass().getName(), scanId, userId);

        if (scanId.isEmpty()) {
            log.warn("scanId 파라미터가 비어있음");
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        try {
            // userId 검증 포함 — 타 사용자의 scanId 로 조회 시 빈 목록 반환
            List<RecipeDTO> rList = analyzeService.analyzeRecipes(scanId, userId);
            log.info("{}.analyzeRecipe End! count={}", this.getClass().getName(), rList.size());
            return ResponseEntity.ok(rList);

        } catch (Exception e) {
            log.error("레시피 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }

    /**
     * 사용자가 레시피 선택 후 영상 요약 요청
     *
     * React → POST /api/analyze/recipe/video-summary?youtube_url=...&scanId=...
     * Spring → FastAPI POST /api/v1/recipes/video-summary
     * FastAPI → Gemini 영상 직접 분석 → List<VideoSummaryDTO> 반환
     */
    @PostMapping("/analyze/recipe/video-summary")
    public ResponseEntity<List<VideoSummaryDTO>> getVideoSummary(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        Integer userId    = Integer.parseInt(jwt.getSubject());
        log.info("{}.getVideoSummary Start! userId={}", this.getClass().getName(), userId);

        String youtubeUrl = CmmUtil.nvl(request.getParameter("youtube_url"));
        String scanId     = CmmUtil.nvl(request.getParameter("scanId"));

        if (youtubeUrl.isEmpty()) {
            log.warn("youtube_url 파라미터가 비어있음");
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        try {
            List<VideoSummaryDTO> rList = analyzeService.getVideoSummary(youtubeUrl, scanId);

            log.info("{}.getVideoSummary End! scanId={} steps={}", this.getClass().getName(), scanId, rList.size());
            for (int i = 0; i < rList.size(); i++) {
                VideoSummaryDTO s = rList.get(i);
                log.info("  [step {}] {} ({}) | {}s ~ {}s | {}",
                        i + 1, s.stepName(), s.displayTime(),
                        s.startSeconds(), s.endSeconds(), s.description());
            }

            return ResponseEntity.ok(rList);

        } catch (Exception e) {
            log.error("영상 요약 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Collections.emptyList());
        }
    }
}
