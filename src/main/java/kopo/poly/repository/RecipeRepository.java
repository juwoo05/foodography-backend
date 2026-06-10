package kopo.poly.repository;

import kopo.poly.repository.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Integer> {

    /** 영상 요약 업데이트 대상 조회 */
    Optional<RecipeEntity> findByScanIdAndYoutubeUrl(String scanId, String youtubeUrl);

    /** 리뷰 작성 드롭다운용 — 해당 유저가 선택했던 레시피 목록 (최신순) */
    List<RecipeEntity> findByUserIdOrderByRegDtDesc(Integer userId);

    /** 쇼핑 가이드용 — 해당 유저의 가장 최신 레시피 1건 */
    Optional<RecipeEntity> findTopByUserIdOrderByRegDtDesc(Integer userId);

    /** 마이페이지 통계 — 유저의 전체 레시피 수 */
    long countByUserId(Integer userId);

    /** 마이페이지 최근 활동 — 최근 3개 레시피 */
    List<RecipeEntity> findTop3ByUserIdOrderByRegDtDesc(Integer userId);
}
