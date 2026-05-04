package kopo.poly.persistance.mongodb;

import com.mongodb.client.model.Indexes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@Slf4j
@RequiredArgsConstructor
public class AbstractMongoDBComon {

    protected boolean createCollection(MongoTemplate mongodb, String colNm) {

        boolean res;

        if (mongodb.collectionExists(colNm)) {
            res = false;

        } else {
            mongodb.createCollection(colNm);
            res = true;
        }

        return res;
    }

    protected boolean createCollection(MongoTemplate mongodb, String colNm, String[] index) {

        log.info("{}.createCollection Start!", this.getClass().getName());

        boolean res = false;

        if (!mongodb.collectionExists(colNm)) {

            if (index.length > 0) {
                mongodb.createCollection(colNm).createIndex(Indexes.ascending(index));

            } else {

                mongodb.createCollection(colNm);

            }

            res = true;
        }

        log.info("{}.createCollection End!", this.getClass().getName());

        return res;
    }

    protected boolean createCollection(MongoTemplate mongodb, String colNm, String index) {

        String[] indexArr = {index};

        return createCollection(mongodb, colNm, indexArr);
    }

    protected boolean dropCollection(MongoTemplate mongodb, String colNm) {

        boolean res = false;

        if (mongodb.collectionExists(colNm)) {
            mongodb.dropCollection(colNm);
            res = true;

        }

        return res;
    }
}
