package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * FastAPI /api/v1/fridge/analyze 응답을 매핑하는 DTO
 * FastAPI → Spring Boot → React 순으로 그대로 전달
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResultDTO(

        // 분석 성공 여부
        boolean success,

        // Roboflow가 감지한 객체 수
        int detectedCount,

        // VLM 분석 결과 목록 (high/low conf 통합)
        List<IngredientDTO> ingredients,

        // 오류 발생 시 메시지
        String errorMessage

) {

    /**
     * 개별 식재료 분석 결과
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IngredientDTO(

            // Roboflow 감지 인덱스 (#1, #2 ...)
            int idx,

            // Roboflow 클래스명 (영문)
            String label,

            // Gemini가 분석한 한국어 식품명
            String name,

            // Roboflow confidence (0.0 ~ 1.0)
            double confidence,

            // 신선도: 좋음 / 보통 / 나쁨
            String freshness,

            // 남은 양: 많음 / 보통 / 적음
            String quantity,

            // 특이사항 (없으면 null)
            String note

    ) {}
}
