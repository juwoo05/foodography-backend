package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * REVIEW 테이블 DTO
 *
 * 저장 요청 (클라이언트 → 서버) : recipeId, rating, comment
 * 목록 응답 (서버 → 클라이언트) : 모든 필드 (JOIN 으로 userName, recipeTitle 포함)
 * userId 는 항상 JWT Subject 에서 주입 — 클라이언트 전달값 무시
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReviewDTO(

        /** PK (목록 조회 시 반환) */
        Integer reviewId,

        /** FK → USER_INFO.USER_ID  (JWT 주입) */
        Integer userId,

        /** 작성자 이름 (USER_INFO.USER_NAME JOIN) */
        String userName,

        /** FK → RECIPE.RECIPE_ID */
        Integer recipeId,

        /** 레시피 제목 (RECIPE.TITLE JOIN) */
        String recipeTitle,

        /** 평점 1 ~ 5 */
        Integer rating,

        /** 리뷰 내용 */
        String comment,

        /** 요리 사진 S3 퍼블릭 URL (nullable) */
        String imageUrl,

        /** 레시피 유튜브 썸네일 URL — RECIPE.YOUTUBE_URL_THUMBNAIL JOIN (nullable) */
        String youtubeUrlThumbnail,

        /** 작성 일시 (yyyy-MM-dd 포맷 문자열) */
        String regDt

) {}
