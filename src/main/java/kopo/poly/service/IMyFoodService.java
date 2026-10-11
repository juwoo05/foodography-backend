package kopo.poly.service;

import kopo.poly.dto.FoodDbDTO;

import java.util.List;

public interface IMyFoodService {

    /**
     * 식품안전처 영양 DB 전체를 페이지네이션으로 수집하여
     * Pinecone VectorStore 에 인덱싱합니다.
     */
    void indexFoodDatabase() throws Exception;

    /**
     * 식품안전처 영양 DB 전체를 페이지네이션으로 수집하여
     * MariaDB(FOOD_NUTRITION) 에만 저장합니다. Pinecone 은 건드리지 않습니다.
     */
    void indexFoodDatabaseMariaOnly() throws Exception;

    /**
     * 식재료명 목록을 Pinecone 유사도 검색 후 Claude 가 최적 식품을 선택하여 반환합니다.
     *
     * <p>scanId 를 Redis 캐시 키로 사용합니다.
     * 동일 scanId 재요청 시 Pinecone · Claude 호출 없이 캐시에서 즉시 반환합니다.</p>
     *
     * @param scanId      FOOD_AFTER 문서의 scanId (캐시 키)
     * @param ingredients 식재료명 목록
     */
    List<FoodDbDTO> matchIngredients(String scanId, List<String> ingredients) throws Exception;
}
