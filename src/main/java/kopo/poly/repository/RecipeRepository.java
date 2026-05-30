package kopo.poly.repository;

import kopo.poly.repository.entity.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Integer> {

    /**
     * 영상 요약 업데이트 대상 조회
     * getVideoSummary 호출 시 scanId + youtubeUrl 로 레시피 특정
     */
    Optional<RecipeEntity> findByScanIdAndYoutubeUrl(String scanId, String youtubeUrl);
}
