package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.*;
import kopo.poly.service.IAnalyzeService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<AnalysisResultDTO> analyzeImage(HttpServletRequest request) {

        log.info("{}.analyzeImage Start!", this.getClass().getName());

        // S3 저장 키 (업로드 완료 후 React가 보관하고 있던 값)
        String savedFilename = CmmUtil.nvl(request.getParameter("filename"));

        log.info("분석 요청 파일: {}", savedFilename);

        if (savedFilename.isEmpty()) {
            log.warn("filename 파라미터가 비어있음");
            return ResponseEntity.badRequest()
                    .body(new AnalysisResultDTO(false, 0, null, "filename 파라미터가 필요합니다.", null));
        }

        AnalysisResultDTO rDTO = analyzeService.analyzeImage(savedFilename);

        if (rDTO != null && rDTO.success()) {
            try {
                int saveRes = analyzeService.saveFoodResult(rDTO);
                log.info("MongoDB 저장 완료 result={}", saveRes);
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
     * React → POST /api/analyze/after  (body: 수정된 AnalysisResultDTO, scanId 포함)
     * Spring → MongoDB FOOD_AFTER 컬렉션 저장
     */
    @PostMapping("/analyze/reviewed")
    public ResponseEntity<MsgDTO> saveReviewedResult(@RequestBody AnalysisResultDTO pDTO) {

        log.info("{}.saveAfterResult Start! scanId={}", this.getClass().getName(), pDTO.scanId());

        MsgDTO rDTO;
        try {
            int res = analyzeService.saveFoodAfterResult(pDTO);
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
     * Spring → FOOD_AFTER 식재료 조회 → FastAPI 레시피 분석 → 결과 반환
     */
    @PostMapping("/analyze/recipe")
    public ResponseEntity<List<RecipeDTO>> analyzeRecipe(HttpServletRequest request) {

        log.info("{}.analyzeRecipe Start!", this.getClass().getName());

        String scanId = CmmUtil.nvl(request.getParameter("scanId"));

        if (scanId.isEmpty()) {
            log.warn("scanId 파라미터가 비어있음");
            return ResponseEntity.badRequest().body(List.of());
        }

        try {
            List<RecipeDTO> rList = analyzeService.analyzeRecipes(scanId);
            log.info("{}.analyzeRecipe End! count={}", this.getClass().getName(), rList.size());
            return ResponseEntity.ok(rList);

        } catch (Exception e) {
            log.error("레시피 분석 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }
}
