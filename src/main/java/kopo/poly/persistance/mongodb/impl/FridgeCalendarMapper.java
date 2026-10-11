package kopo.poly.persistance.mongodb.impl;

import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IFridgeCalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class FridgeCalendarMapper extends AbstractMongoDBComon implements IFridgeCalendarMapper {

    private final MongoTemplate mongodb;

    @Override
    public List<Document> getScansBetween(String colNm, Integer userId, Instant start, Instant end) throws Exception {

        log.info("{}.getScansBetween Start! colNm={} userId={} start={} end={}",
                this.getClass().getName(), colNm, userId, start, end);

        // ObjectId(Date) → 해당 시각의 최소 ObjectId (나머지 바이트 0) — _id 범위로 생성 시각 필터
        Query query = new Query(Criteria.where("userId").is(userId)
                .and("_id").gte(new ObjectId(Date.from(start))).lt(new ObjectId(Date.from(end))))
                .with(Sort.by(Sort.Direction.ASC, "_id"));

        // 달력에는 좌표가 필요 없음 — 응답 크기 축소
        query.fields().exclude("ingredients.polygon");

        List<Document> rList = mongodb.find(query, Document.class, colNm);

        log.info("{}.getScansBetween End! count={}", this.getClass().getName(), rList.size());

        return rList;
    }
}
