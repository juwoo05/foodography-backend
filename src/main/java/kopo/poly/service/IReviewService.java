package kopo.poly.service;

import kopo.poly.dto.RecipeDTO;
import kopo.poly.dto.ReviewDTO;

import java.util.List;

public interface IReviewService {

    /**
     * 리뷰 작성 드롭다운용 — 해당 유저가 선택했던 레시피 목록
     * RECIPE.USER_ID = userId 조건 (getVideoSummary 호출 시 저장됨)
     */
    List<RecipeDTO> getMyRecipes(Integer userId) throws Exception;

    /**
     * 전체 리뷰 목록 조회 (작성자명 + 레시피명 JOIN 포함)
     */
    List<ReviewDTO> getReviewList() throws Exception;

    /**
     * 리뷰 저장
     * userId 는 컨트롤러에서 JWT 기반으로 주입되어 전달됨
     */
    void saveReview(ReviewDTO dto) throws Exception;
}
