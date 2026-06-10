package kopo.poly.service.impl;

import kopo.poly.dto.MyPageDTO;
import kopo.poly.repository.RecipeRepository;
import kopo.poly.repository.ReviewRepository;
import kopo.poly.repository.ShoppingRepository;
import kopo.poly.repository.UserInfoRepository;
import kopo.poly.repository.entity.RecipeEntity;
import kopo.poly.repository.entity.UserInfoEntity;
import kopo.poly.service.IMyPageService;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService implements IMyPageService {

    private final UserInfoRepository  userInfoRepository;
    private final RecipeRepository    recipeRepository;
    private final ReviewRepository    reviewRepository;
    private final ShoppingRepository  shoppingRepository;

    @Override
    public MyPageDTO getMyPageInfo(Integer userId) throws Exception {

        log.info("{}.getMyPageInfo Start | userId={}", getClass().getName(), userId);

        // ── 유저 기본 정보 ──────────────────────────────────────────────
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다. userId=" + userId));

        // 이메일은 AES128-CBC 로 암호화되어 저장되므로 복호화 처리
        String email;
        try {
            email = EncryptUtil.decAES128CBC(user.getEmail());
        } catch (Exception e) {
            log.warn("이메일 복호화 실패 | userId={} error={}", userId, e.getMessage());
            email = "";   // 복호화 실패 시 빈 문자열 반환
        }

        // ── 활동 통계 ───────────────────────────────────────────────────
        long recipeCount = recipeRepository.countByUserId(userId);
        long reviewCount = reviewRepository.countByUserId(userId);
        long cartCount   = shoppingRepository.countByUserId(userId);

        // ── 최근 레시피 3건 ─────────────────────────────────────────────
        List<MyPageDTO.RecentRecipeDTO> recentRecipes = recipeRepository
                .findTop3ByUserIdOrderByRegDtDesc(userId)
                .stream()
                .map(this::toRecentRecipe)
                .toList();

        // ── 최근 리뷰 3건 (레시피 제목 포함) ───────────────────────────
        List<MyPageDTO.RecentReviewDTO> recentReviews = reviewRepository
                .findTop3ByUserIdWithRecipeTitle(userId)
                .stream()
                .map(this::toRecentReview)
                .toList();

        log.info("{}.getMyPageInfo End | recipeCount={} reviewCount={} cartCount={}",
                getClass().getName(), recipeCount, reviewCount, cartCount);

        return MyPageDTO.builder()
                .userId     (user.getUserId())
                .userName   (user.getUserName())
                .email      (email)               // 복호화된 이메일
                .phoneNum   (user.getPhoneNum())
                .regDt      (user.getRegDt())
                .recipeCount(recipeCount)
                .reviewCount(reviewCount)
                .cartCount  (cartCount)
                .recentRecipes(recentRecipes)
                .recentReviews(recentReviews)
                .build();
    }

    // ── 내부 변환 헬퍼 ─────────────────────────────────────────────────

    private MyPageDTO.RecentRecipeDTO toRecentRecipe(RecipeEntity e) {
        return MyPageDTO.RecentRecipeDTO.builder()
                .recipeId          (e.getRecipeId())
                .title             (e.getTitle())
                .difficulty        (e.getDifficulty())
                .cookingTime       (e.getCookingTime())
                .calories          (e.getCalories())
                .youtubeUrlThumbnail(e.getYoutubeUrlThumbnail())
                .regDt             (e.getRegDt())
                .build();
    }

    private MyPageDTO.RecentReviewDTO toRecentReview(Object[] row) {
        // [0] REVIEW_ID, [1] RECIPE_ID, [2] TITLE, [3] RATING,
        // [4] COMMENT,   [5] IMAGE_URL, [6] REG_DT
        return MyPageDTO.RecentReviewDTO.builder()
                .reviewId   (row[0] != null ? ((Number) row[0]).intValue() : null)
                .recipeId   (row[1] != null ? ((Number) row[1]).intValue() : null)
                .recipeTitle(row[2] != null ? (String) row[2] : null)
                .rating     (row[3] != null ? ((Number) row[3]).intValue() : null)
                .comment    (row[4] != null ? (String) row[4] : null)
                .imageUrl   (row[5] != null ? (String) row[5] : null)
                .regDt      (row[6] != null ? ((java.sql.Timestamp) row[6]).toLocalDateTime() : null)
                .build();
    }
}
