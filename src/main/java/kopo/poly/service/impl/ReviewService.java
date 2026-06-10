package kopo.poly.service.impl;

import kopo.poly.dto.RecipeDTO;
import kopo.poly.dto.ReviewDTO;
import kopo.poly.repository.RecipeRepository;
import kopo.poly.repository.ReviewRepository;
import kopo.poly.repository.entity.ReviewEntity;
import kopo.poly.service.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService implements IReviewService {

    private final ReviewRepository  reviewRepository;
    private final RecipeRepository  recipeRepository;

    // ── 내 레시피 목록 (드롭다운) ──────────────────────────────────────────

    @Override
    public List<RecipeDTO> getMyRecipes(Integer userId) throws Exception {

        log.info("{}.getMyRecipes Start | userId={}", this.getClass().getName(), userId);

        List<RecipeDTO> result = recipeRepository.findByUserIdOrderByRegDtDesc(userId)
                .stream()
                .map(e -> RecipeDTO.builder()
                        .recipeId(e.getRecipeId())
                        .title(e.getTitle())
                        .youtube_url_thumbnail(e.getYoutubeUrlThumbnail())
                        .build())
                .toList();

        log.info("{}.getMyRecipes End | count={}", this.getClass().getName(), result.size());
        return result;
    }

    // ── 전체 리뷰 목록 ────────────────────────────────────────────────────

    @Override
    public List<ReviewDTO> getReviewList() throws Exception {

        log.info("{}.getReviewList Start", this.getClass().getName());

        List<Object[]> rows = reviewRepository.findAllWithDetails();

        // Object[] 컬럼 순서: REVIEW_ID(0) USER_ID(1) USER_NAME(2)
        //                     RECIPE_ID(3) TITLE(4) RATING(5) COMMENT(6) REG_DT(7)
        //                     IMAGE_URL(8) YOUTUBE_URL_THUMBNAIL(9)
        List<ReviewDTO> result = rows.stream()
                .map(row -> ReviewDTO.builder()
                        .reviewId            (((Number) row[0]).intValue())
                        .userId              (((Number) row[1]).intValue())
                        .userName            ((String)  row[2])
                        .recipeId            (((Number) row[3]).intValue())
                        .recipeTitle         ((String)  row[4])
                        .rating              (((Number) row[5]).intValue())
                        .comment             ((String)  row[6])
                        // DATETIME → Timestamp.toString() = "yyyy-MM-dd HH:mm:ss.n" → 앞 10자
                        .regDt               (row[7] != null ? row[7].toString().substring(0, 10) : "")
                        .imageUrl            (row[8] != null ? (String) row[8] : null)
                        .youtubeUrlThumbnail (row[9] != null ? (String) row[9] : null)
                        .build())
                .toList();

        log.info("{}.getReviewList End | count={}", this.getClass().getName(), result.size());
        return result;
    }

    // ── 리뷰 저장 ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void saveReview(ReviewDTO dto) throws Exception {

        log.info("{}.saveReview Start | userId={} recipeId={} rating={}",
                this.getClass().getName(), dto.userId(), dto.recipeId(), dto.rating());

        ReviewEntity entity = ReviewEntity.builder()
                .userId  (dto.userId())
                .recipeId(dto.recipeId())
                .rating  (dto.rating())
                .comment (dto.comment())
                .imageUrl(dto.imageUrl())
                .build();

        reviewRepository.save(entity);

        log.info("{}.saveReview End | reviewId={}", this.getClass().getName(), entity.getReviewId());
    }
}
