package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 네이버 쇼핑 검색 결과 및 MYCART 저장 항목을 공용으로 표현하는 DTO
 *
 * <ul>
 *   <li>검색 결과 응답 : cartId·userId 없이 반환</li>
 *   <li>장바구니 조회 응답 : 모든 필드 포함</li>
 *   <li>장바구니 저장 요청 : cartId 없이 클라이언트가 전달</li>
 * </ul>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShoppingDTO(

        /** MYCART PK — 저장 후 응답 시 포함 */
        Integer cartId,

        /** FK → USER_INFO.USER_ID (JWT 주입) */
        Integer userId,

        /** FK → RECIPE.RECIPE_ID — 검색 기준 레시피 */
        Integer recipeId,

        /** 검색한 재료명 (ADDITIONAL_INGREDIENTS 항목) */
        String ingredient,

        /** 정렬 기준 — "기본" | "최저가" | "최고가" */
        String sortType,

        /** 상품명 (HTML 태그 제거 후) */
        String title,

        /** 상품 상세 링크 */
        String link,

        /** 상품 대표 이미지 URL */
        String image,

        /** 최저가 (문자열) */
        String lprice,

        /** 쇼핑몰명 */
        String mallName

) {}
