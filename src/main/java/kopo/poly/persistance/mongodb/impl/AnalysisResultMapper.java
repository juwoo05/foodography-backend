package kopo.poly.persistance.mongodb.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.persistance.mongodb.AbstractMongoDBComon;
import kopo.poly.persistance.mongodb.IAnalysisResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class AnalysisResultMapper extends AbstractMongoDBComon implements IAnalysisResultMapper {

    private final MongoTemplate mongodb;

    @Override
    public int insertDate(AnalysisResultDTO pDTO, String colNm) throws MongoException {

        log.info("{}.insertData Start!", this.getClass().getName());

        int res;

        if (super.createCollection(mongodb, colNm)) {
            log.info("{} 생성되었습니다", colNm);
        }

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        col.insertOne(new Document(new ObjectMapper().convertValue(pDTO, Map.class)));

        res = 1;

        log.info("{}.insertData End!", this.getClass().getName());

        return res;
    }

    @Override
    public List<String> getFoodResult(String colNm, String scanId) throws Exception {

        log.info("{}.getFoodResult Start! colNm={} scanId={}", this.getClass().getName(), colNm, scanId);

        MongoCollection<Document> col = mongodb.getCollection(colNm);

        // 특정 scanId 도큐먼트의 ingredients.name distinct 조회
        List<String> rList = new ArrayList<>();
        col.distinct("ingredients.name", String.class)
                .filter(new Document("scanId", scanId)
                        .append("ingredients.name", new Document("$exists", true).append("$ne", null)))
                .into(rList);

        log.info("{}.getFoodResult End! scanId={} result count={}", this.getClass().getName(), scanId, rList.size());

        return rList;
    }
}

