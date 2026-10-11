package kopo.poly.service.impl;

import kopo.poly.dto.FridgeScanDTO;
import kopo.poly.persistance.mongodb.IFridgeCalendarMapper;
import kopo.poly.service.IFridgeCalendarService;
import kopo.poly.util.FoodCategoryUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class FridgeCalendarService implements IFridgeCalendarService {

    private static final String COL_NM   = "FOOD_AFTER";
    private static final ZoneId KST      = ZoneId.of("Asia/Seoul");
    private static final int    MAX_DAYS = 62;   // 월간 달력(최대 6주 = 42일) + 여유

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IFridgeCalendarMapper fridgeCalendarMapper;

    @Override
    public List<FridgeScanDTO> getScans(Integer userId, String start, String end) throws Exception {

        log.info("{}.getScans Start! userId={} start={} end={}", this.getClass().getName(), userId, start, end);

        LocalDate startDate = parseDate(start);
        LocalDate endDate   = parseDate(end);
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        if (days <= 0 || days > MAX_DAYS) {
            throw new IllegalArgumentException("조회 기간은 1~" + MAX_DAYS + "일이어야 합니다.");
        }

        List<Document> docs = fridgeCalendarMapper.getScansBetween(
                COL_NM, userId,
                startDate.atStartOfDay(KST).toInstant(),
                endDate.atStartOfDay(KST).toInstant());

        // 같은 scanId 를 여러 번 저장한 경우 — 시각은 최초 저장, 내용은 최신 저장본 사용 (_id 오름차순이라 put 이 최신으로 덮어씀)
        Map<String, ObjectId> firstIds = new LinkedHashMap<>();
        Map<String, Document> latest   = new LinkedHashMap<>();
        for (Document doc : docs) {
            ObjectId id = doc.getObjectId("_id");
            String scanId = doc.getString("scanId");
            String key = scanId == null || scanId.isBlank() ? id.toHexString() : scanId;
            firstIds.putIfAbsent(key, id);
            latest.put(key, doc);
        }

        List<FridgeScanDTO> rList = new ArrayList<>();
        for (Map.Entry<String, ObjectId> e : firstIds.entrySet()) {
            ZonedDateTime scanAt = e.getValue().getDate().toInstant().atZone(KST);
            List<FridgeScanDTO.Item> items = toItems(latest.get(e.getKey()));

            rList.add(FridgeScanDTO.builder()
                    .scanId(e.getKey())
                    .scanDt(scanAt.format(DT_FMT))
                    .date(scanAt.format(DATE_FMT))
                    .itemCount(items.size())
                    .items(items)
                    .build());
        }

        log.info("{}.getScans End! docCount={} scanCount={}", this.getClass().getName(), docs.size(), rList.size());

        return rList;
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다: " + value);
        }
    }

    /** ingredients 배열 → Item 목록 (이름 없는 항목 제외, 분류 부여) */
    private static List<FridgeScanDTO.Item> toItems(Document doc) {

        List<FridgeScanDTO.Item> items = new ArrayList<>();
        List<?> ingArr = doc.getList("ingredients", Object.class);
        if (ingArr == null) {
            return items;
        }

        for (Object o : ingArr) {
            if (!(o instanceof Document ing)) {
                continue;
            }
            String name = str(ing.get("name"));
            if (name == null) {
                continue;
            }
            String note = str(ing.get("note"));

            items.add(FridgeScanDTO.Item.builder()
                    .name(name)
                    .quantity(str(ing.get("quantity")))
                    .freshness(str(ing.get("freshness")))
                    .stockStatus(str(ing.get("stock_status")))
                    .note(note)
                    .category(FoodCategoryUtil.classify(name, note))
                    .build());
        }
        return items;
    }

    /** 문자열·숫자 혼재 필드 → 공백 제거 문자열 (빈 값은 null) */
    private static String str(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        return s.isEmpty() ? null : s;
    }
}
