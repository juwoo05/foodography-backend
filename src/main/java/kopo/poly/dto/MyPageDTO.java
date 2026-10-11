package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 응답 DTO
 * — 유저 기본 정보 + 활동 통계 + 최근 레시피·리뷰
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MyPageDTO(

        // ── 유저 기본 정보 ─────────────────────────────────────────
        Integer       userId,
        String        userName,
        String        email,
        String        phoneNum,
        String        address,
        LocalDateTime regDt,

        // ── 활동 통계 ──────────────────────────────────────────────
        long recipeCount,   // 분석한 레시피 수
        long reviewCount,   // 작성한 리뷰 수
        long cartCount,     // 장바구니 담은 수

        // ── 최근 레시피 (최대 3건) ─────────────────────────────────
        List<RecentRecipeDTO> recentRecipes,

        // ── 최근 리뷰 (최대 3건) ──────────────────────────────────
        List<RecentReviewDTO> recentReviews

) {

    /** 최근 레시피 요약 */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RecentRecipeDTO(
            Integer       recipeId,
            String        title,
            String        difficulty,
            Integer       cookingTime,
            Integer       calories,
            String        youtubeUrlThumbnail,
            LocalDateTime regDt
    ) {}

    /** 최근 리뷰 요약 (레시피 제목 포함) */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RecentReviewDTO(
            Integer       reviewId,
            Integer       recipeId,
            String        recipeTitle,
            Integer       rating,
            String        comment,
            String        imageUrl,
            LocalDateTime regDt
    ) {}
}
