package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 제철 식재료 페이지 — 이달의 식재료 카드 DTO
 *
 * <p>농사로 이달의 음식 API(monthFdmtLst + monthFdmtDtl) 응답을 카드 한 장 단위로 담습니다.</p>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeasonalFoodDTO(

        /** 농사로 콘텐츠 번호 — 카드 key */
        String cntntsNo,

        /** 식재료명 */
        String name,

        /** 분류 — "fruit" | "vegetable" | "seafood" | "grain" | "mushroom" (이름 키워드로 서버에서 분류) */
        String category,

        /** 제철 월 (요청 month) */
        Integer month,

        /** 대표 이미지 URL — 없으면 프론트에서 분류 아이콘 표시 */
        String imageUrl,

        /** 식재료 소개·유래 */
        String intro,

        /** 품종특성·구입요령 */
        String buyTip,

        /** 보관법 */
        String storage,

        /** 손질법 */
        String prep,

        /** 영양성분·효능 */
        String nutrition

) {}
