package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.service.IAnalyzeService;
import kopo.poly.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                    .body(new AnalysisResultDTO(false, 0, null, "filename 파라미터가 필요합니다."));
        }

        AnalysisResultDTO rDTO = analyzeService.analyzeImage(savedFilename);

        log.info("{}.analyzeImage End!", this.getClass().getName());

        return ResponseEntity.ok(rDTO);
    }
}
