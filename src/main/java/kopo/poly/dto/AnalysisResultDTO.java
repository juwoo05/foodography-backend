package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
            String note,

            // Roboflow 세그멘테이션 폴리곤 좌표 목록
            // FastAPI가 [{"x": 120.0, "y": 45.0}, ...] 형태로 전달
            List<PolygonPointDTO> polygon,

            // Gemini 재고 상태 텍스트 (많음 / 보통 / 적음)
            // FastAPI 필드명이 stock_status이므로 @JsonProperty로 매핑
            @JsonProperty("stock_status")
            String stockStatus

    ) {}

    /**
     * 폴리곤 꼭짓점 좌표 — Roboflow 원본 픽셀 기준
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PolygonPointDTO(
            double x,
            double y
    ) {}
}