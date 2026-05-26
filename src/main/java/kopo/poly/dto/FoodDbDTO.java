package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 식품 영양 DB 매칭 파이프라인 전용 DTO
 *
 * <ul>
 *   <li>MongoDB FOOD_AFTER → 식재료명 추출 시 사용</li>
 *   <li>식품안전처 API 응답 파싱 시 사용 (배치 인덱싱)</li>
 *   <li>Pinecone 유사도 검색 + Claude 매칭 결과 반환 시 사용</li>
 * </ul>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FoodDbDTO(

        /* ── MongoDB FOOD_AFTER ─────────────────────────────────── */

        /** 현재 분석 세션 ID (FOOD_AFTER.scanId) */
        String scanId,

        /** FOOD_AFTER 에서 추출한 식재료명 목록 (컨트롤러 요청 입력값) */
        List<String> ingredients,

        /* ── 식품안전처 API 원본 필드 ─────────────────────────────── */

        /** 식품 코드 (FOOD_CD) */
        String foodCd,

        /** 한국어 식품명 (FOOD_NM_KR) — VectorStore content 텍스트로 사용 */
        String foodNm,

        /** 열량 kcal (AMT_NUM1) */
        Double kcal,

        /** 단백질 g (AMT_NUM3) */
        Double protein,

        /** 지방 g (AMT_NUM4) */
        Double fat,

        /** 탄수화물 g (AMT_NUM7) */
        Double carbs,

        /* ── Claude 매칭 결과 ─────────────────────────────────────── */

        /** 매칭 대상 식재료명 (사용자 입력 단일 항목) */
        String ingredient,

        /** Claude 가 선택한 가장 유사한 공식 식품명 */
        String matchedFoodNm,

        /** 매칭된 식품 열량 */
        Double matchedKcal,

        /** 매칭된 식품 단백질 */
        Double matchedProtein,

        /** 매칭된 식품 지방 */
        Double matchedFat,

        /** 매칭된 식품 탄수화물 */
        Double matchedCarbs

) {
}
