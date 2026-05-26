package kopo.poly.persistance.mongodb;

import kopo.poly.dto.FoodDbDTO;

public interface IMyFoodMapper {

    /**
     * FOOD_AFTER 컬렉션에서 가장 최근 스캔의 식재료명 목록을 조회합니다.
     *
     * <p>_id 기준 내림차순으로 최신 도큐먼트 1건을 가져온 뒤
     * {@code ingredients[].name} 배열을 {@link FoodDbDTO#ingredients()} 에 담아 반환합니다.</p>
     *
     * @param colNm MongoDB 컬렉션명 (FOOD_AFTER)
     * @return {@code scanId} + {@code ingredients} 가 채워진 {@link FoodDbDTO}
     * @throws Exception MongoDB 조회 오류 시
     */
    FoodDbDTO getLatestIngredients(String colNm) throws Exception;
}
