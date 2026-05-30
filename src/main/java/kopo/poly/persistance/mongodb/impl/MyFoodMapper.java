package kopo.poly.persistance.mongodb.impl;

import kopo.poly.dto.FoodDbDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IMyFoodMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MyFoodMapper extends AbstractMongoDBComon implements IMyFoodMapper {

    private final MongoTemplate mongodb;

    /**
     * FOOD_AFTER 컬렉션에서 {@code _id} 기준 가장 최근 도큐먼트 1건을 조회합니다.
     *
     * <p>도큐먼트 내 {@code ingredients} 배열의 {@code name} 필드를 순회하여
     * null·공백을 제거한 식재료명 목록을 {@link FoodDbDTO#ingredients()} 에 담습니다.</p>
     */
    @Override
    public FoodDbDTO getLatestIngredients(String colNm, Integer userId) throws Exception {

        log.info("{}.getLatestIngredients Start! colNm={} userId={}", this.getClass().getName(), colNm, userId);

        // userId 필터 + _id 내림차순 → 해당 사용자의 가장 최근 도큐먼트 1건
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "_id"))
                .limit(1);

        Document doc = mongodb.findOne(query, Document.class, colNm);

        if (doc == null) {
            log.warn("{}.getLatestIngredients 도큐먼트 없음 colNm={}",
                    this.getClass().getName(), colNm);
            return FoodDbDTO.builder().ingredients(List.of()).build();
        }

        String scanId = doc.getString("scanId");

        // ingredients 배열 순회 → name 값 추출 (null·공백 제외)
        List<String> names = new ArrayList<>();
        List<?> ingArr = doc.getList("ingredients", Object.class);

        if (ingArr != null) {
            for (Object item : ingArr) {
                if (item instanceof Document ingDoc) {
                    String name = ingDoc.getString("name");
                    if (name != null && !name.isBlank()) {
                        names.add(name);
                    }
                }
            }
        }

        FoodDbDTO result = FoodDbDTO.builder()
                .scanId(scanId)
                .ingredients(names)
                .build();

        log.info("{}.getLatestIngredients End! colNm={} scanId={} ingredientCount={}",
                this.getClass().getName(), colNm, scanId, names.size());

        return result;
    }
}
