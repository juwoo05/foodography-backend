package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 냉장고 달력 — 스캔 1건 (FOOD_AFTER 사용자 확정본 기준)
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FridgeScanDTO(

        /** 스캔 ID (FOOD_BEFORE·FOOD_AFTER 연결 키) */
        String scanId,

        /** 스캔 시각 (KST, yyyy-MM-dd HH:mm) — 최초 저장 시각 */
        String scanDt,

        /** 달력 배치용 날짜 (KST, yyyy-MM-dd) */
        String date,

        /** 식료품 수 */
        Integer itemCount,

        /** 식료품 목록 */
        List<Item> items
) {

    /**
     * 스캔된 식료품 1개
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Item(

            /** 식재료명 (한글) */
            String name,

            /** 수량 */
            String quantity,

            /** 신선도 (좋음/보통/나쁨/알 수 없음) */
            String freshness,

            /** 재고 상태 */
            String stockStatus,

            /** 메모 (예: 양념·부재료) */
            String note,

            /** 분류 (VEGETABLE, FRUIT, MEAT, SEAFOOD, DAIRY_EGG, PROCESSED, SEASONING, ETC) */
            String category
    ) {
    }
}
