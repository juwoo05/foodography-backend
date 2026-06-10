package kopo.poly.controller;

import kopo.poly.dto.RecipeDTO;
import kopo.poly.dto.ReviewDTO;
import kopo.poly.response.CommonResponse;
import kopo.poly.service.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final IReviewService reviewService;

    /**
     * GET /api/review/my-recipes
     * 리뷰 작성 드롭다운용 — 현재 유저가 선택했던 레시피 목록
     */
    @GetMapping("/my-recipes")
    public ResponseEntity<List<RecipeDTO>> getMyRecipes(
            @AuthenticationPrincipal Jwt jwt) throws Exception {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.getMyRecipes Start | userId={}", this.getClass().getName(), userId);

        List<RecipeDTO> rList = reviewService.getMyRecipes(userId);

        log.info("{}.getMyRecipes End | count={}", this.getClass().getName(), rList.size());
        return ResponseEntity.ok(rList);
    }

    /**
     * GET /api/review/list
     * 전체 리뷰 목록 (작성자명 + 레시피명 포함)
     */
    @GetMapping("/list")
    public ResponseEntity<List<ReviewDTO>> getReviewList(
            @AuthenticationPrincipal Jwt jwt) throws Exception {

        log.info("{}.getReviewList Start", this.getClass().getName());

        List<ReviewDTO> rList = reviewService.getReviewList();

        log.info("{}.getReviewList End | count={}", this.getClass().getName(), rList.size());
        return ResponseEntity.ok(rList);
    }

    /**
     * POST /api/review
     * 리뷰 저장
     * 요청 바디: { recipeId, rating, comment }
     * userId 는 JWT Subject 에서 추출 — 클라이언트 전달값 무시
     */
    @PostMapping
    public ResponseEntity<CommonResponse<?>> saveReview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ReviewDTO dto) throws Exception {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.saveReview Start | userId={} recipeId={}", this.getClass().getName(), userId, dto.recipeId());

        ReviewDTO withUser = ReviewDTO.builder()
                .userId  (userId)
                .recipeId(dto.recipeId())
                .rating  (dto.rating())
                .comment (dto.comment())
                .imageUrl(dto.imageUrl())
                .build();

        reviewService.saveReview(withUser);

        log.info("{}.saveReview End | userId={}", this.getClass().getName(), userId);
        return ResponseEntity.ok(
                CommonResponse.of(HttpStatus.OK, "리뷰가 등록되었습니다.", null)
        );
    }
}
