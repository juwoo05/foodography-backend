package kopo.poly.repository;

import kopo.poly.repository.entity.FoodNutritionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FoodNutritionRepository extends JpaRepository<FoodNutritionEntity, String> {

    /** 식품명으로 조회 — foodCd 로 찾지 못할 경우 폴백 */
    Optional<FoodNutritionEntity> findByFoodNm(String foodNm);
}
