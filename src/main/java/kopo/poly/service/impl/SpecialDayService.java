package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.SpecialDayDTO;
import kopo.poly.service.ISpecialDayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpecialDayService implements ISpecialDayService {

    private final WebClient    webClient;
    private final ObjectMapper objectMapper;

    @Value("${kasi.spcde.base-url}")
    private String baseUrl;

    @Value("${kasi.spcde.api-key}")
    private String apiKey;              // 공공데이터포털 키 (Decoding·Encoding 모두 가능)

    /** 외부 API 단일 호출 타임아웃 */
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    /** 분류 → 오퍼레이션 (순서 = 같은 날·같은 이름 중복 시 우선순위) */
    private static final Map<String, String> OPERATIONS = new LinkedHashMap<>();

    static {
        OPERATIONS.put("HOLIDAY",     "getRestDeInfo");
        OPERATIONS.put("SUNDRY",      "getSundryDayInfo");
        OPERATIONS.put("DIVISION",    "get24DivisionsInfo");
        OPERATIONS.put("ANNIVERSARY", "getAnniversaryInfo");
    }

    private static final List<String> KIND_ORDER = List.copyOf(OPERATIONS.keySet());

    @Override
    @Cacheable(value = "specialDays", key = "#year", unless = "#result.isEmpty()")
    public List<SpecialDayDTO> getSpecialDays(int year) {

        log.info("{}.getSpecialDays Start! year={}", getClass().getName(), year);

        // 4개 오퍼레이션 병렬 호출 — 실패한 분류만 빈 목록
        List<SpecialDayDTO> all = OPERATIONS.entrySet().parallelStream()
                .flatMap(e -> fetch(e.getKey(), e.getValue(), year).stream())
                .toList();

        // 같은 날·같은 이름은 우선순위가 높은 분류 하나만 (예: 공휴일 추석 ↔ 기념일 추석)
        Map<String, SpecialDayDTO> merged = new LinkedHashMap<>();
        all.stream()
                .sorted(Comparator.comparingInt(d -> KIND_ORDER.indexOf(d.kind())))
                .forEach(d -> merged.putIfAbsent(d.date() + "|" + d.name(), d));

        // ArrayList 로 반환 — Stream.toList() 는 Redis 캐시 역직렬화 실패 (CacheConfig NON_FINAL 타이핑)
        List<SpecialDayDTO> result = merged.values().stream()
                .sorted(Comparator.comparing(SpecialDayDTO::date)
                        .thenComparingInt(d -> KIND_ORDER.indexOf(d.kind())))
                .collect(Collectors.toCollection(ArrayList::new));

        log.info("{}.getSpecialDays End! year={} count={}", getClass().getName(), year, result.size());

        return result;
    }

    /** 오퍼레이션 1개 호출 → 특일 목록 (오류 시 빈 목록) */
    private List<SpecialDayDTO> fetch(String kind, String operation, int year) {

        // 키를 직접 인코딩 후 URI.create 로 이중 인코딩 방지 — Encoding 키('%' 포함)는 그대로 사용
        String encodedKey = apiKey.contains("%") ? apiKey : URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String apiUrl = String.format("%s/%s?serviceKey=%s&solYear=%d&numOfRows=100&pageNo=1&_type=json",
                baseUrl, operation, encodedKey, year);

        try {
            String body;
            try {
                body = webClient.get()
                        .uri(URI.create(apiUrl))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(API_TIMEOUT)
                        .block();
            } catch (WebClientResponseException e) {
                // 미등록 키는 403 + OpenAPI_ServiceResponse 본문 — 아래 공통 처리로 원인 로그
                body = e.getResponseBodyAsString(StandardCharsets.UTF_8);
            }

            if (body == null || body.isBlank() || body.trim().startsWith("<")) {
                log.warn("특일 정보 응답 오류 | op={} body={}", operation, body);
                return List.of();
            }

            JsonNode json = objectMapper.readTree(body);

            // 인증키 오류 (예: SERVICE_KEY_IS_NOT_REGISTERED_ERROR — 활용신청 전·승인 반영 전)
            JsonNode authErr = json.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
            if (!authErr.isMissingNode()) {
                log.warn("특일 정보 인증 오류 | op={} {} ({})", operation,
                        authErr.path("errMsg").asText(), authErr.path("returnAuthMsg").asText());
                return List.of();
            }

            JsonNode root       = json.path("response");
            String   resultCode = root.path("header").path("resultCode").asText();
            if (!"00".equals(resultCode)) {
                log.warn("특일 정보 resultCode={} msg={} op={}", resultCode,
                        root.path("header").path("resultMsg").asText(), operation);
                return List.of();
            }

            // 결과 0건이면 items 가 빈 문자열, 1건이면 item 이 배열이 아닌 객체
            JsonNode item = root.path("body").path("items").path("item");
            List<JsonNode> nodes = new ArrayList<>();
            if (item.isArray()) {
                item.forEach(nodes::add);
            } else if (item.isObject()) {
                nodes.add(item);
            }

            List<SpecialDayDTO> rList = new ArrayList<>();
            for (JsonNode n : nodes) {
                String locdate = n.path("locdate").asText();   // yyyyMMdd (숫자)
                String name    = n.path("dateName").asText().trim();
                if (locdate.length() != 8 || name.isEmpty()) {
                    continue;
                }
                rList.add(SpecialDayDTO.builder()
                        .date(locdate.substring(0, 4) + "-" + locdate.substring(4, 6) + "-" + locdate.substring(6, 8))
                        .name(name)
                        .kind(kind)
                        .holiday("Y".equalsIgnoreCase(n.path("isHoliday").asText()))
                        .build());
            }

            log.info("특일 정보 | op={} year={} count={}", operation, year, rList.size());
            return rList;

        } catch (Exception e) {
            log.warn("특일 정보 호출 실패 | op={} year={} {}", operation, year, e.getMessage());
            return List.of();
        }
    }
}
