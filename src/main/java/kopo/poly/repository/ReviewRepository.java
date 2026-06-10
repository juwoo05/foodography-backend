package kopo.poly.repository;

import kopo.poly.repository.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

    /**
     * 전체 리뷰 목록 최신순 조회 — USER_INFO, RECIPE 조인하여 작성자명·레시피명·썸네일 포함
     *
     * 반환 순서: [0] REVIEW_ID, [1] USER_ID, [2] USER_NAME,
     *            [3] RECIPE_ID, [4] TITLE, [5] RATING, [6] COMMENT, [7] REG_DT,
     *            [8] IMAGE_URL, [9] YOUTUBE_URL_THUMBNAIL
     */
    @Query(value = """
            SELECT r.REVIEW_ID,
                   r.USER_ID,
                   u.USER_NAME,
                   r.RECIPE_ID,
                   rec.TITLE,
                   r.RATING,
                   r.COMMENT,
                   r.REG_DT,
                   r.IMAGE_URL,
                   rec.YOUTUBE_URL_THUMBNAIL
            FROM   REVIEW     r
            JOIN   USER_INFO  u   ON r.USER_ID   = u.USER_ID
            JOIN   RECIPE     rec ON r.RECIPE_ID = rec.RECIPE_ID
            ORDER  BY r.REG_DT DESC
            """, nativeQuery = true)
    List<Object[]> findAllWithDetails();

    /** 마이페이지 통계 — 유저의 전체 리뷰 수 */
    long countByUserId(Integer userId);

    /**
     * 마이페이지 최근 리뷰 3건 — 레시피 제목 포함
     * 반환: [0] REVIEW_ID, [1] RECIPE_ID, [2] TITLE, [3] RATING,
     *       [4] COMMENT, [5] IMAGE_URL, [6] REG_DT
     */
    @Query(value = """
            SELECT r.REVIEW_ID,
                   r.RECIPE_ID,
                   rec.TITLE,
                   r.RATING,
                   r.COMMENT,
                   r.IMAGE_URL,
                   r.REG_DT
            FROM   REVIEW  r
            JOIN   RECIPE  rec ON r.RECIPE_ID = rec.RECIPE_ID
            WHERE  r.USER_ID = :userId
            ORDER  BY r.REG_DT DESC
            LIMIT  3
            """, nativeQuery = true)
    List<Object[]> findTop3ByUserIdWithRecipeTitle(@Param("userId") Integer userId);
}
