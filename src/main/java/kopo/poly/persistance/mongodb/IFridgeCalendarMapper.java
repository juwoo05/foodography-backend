package kopo.poly.persistance.mongodb;

import org.bson.Document;

import java.time.Instant;
import java.util.List;

public interface IFridgeCalendarMapper {

    /**
     * 기간 내 사용자의 스캔 확정 도큐먼트 조회
     *
     * <p>도큐먼트에 날짜 필드가 없으므로 {@code _id}(ObjectId) 생성 시각으로 기간을 거릅니다.
     * {@code _id} 오름차순, polygon 좌표는 제외합니다.</p>
     *
     * @param colNm  MongoDB 컬렉션명 (FOOD_AFTER)
     * @param userId 사용자 ID
     * @param start  시작 시각 (포함)
     * @param end    종료 시각 (제외)
     * @return 도큐먼트 목록 (없으면 빈 목록)
     * @throws Exception MongoDB 조회 오류 시
     */
    List<Document> getScansBetween(String colNm, Integer userId, Instant start, Instant end) throws Exception;
}
