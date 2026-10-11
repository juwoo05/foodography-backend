package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kopo.poly.dto.SeasonalFoodDTO;
import kopo.poly.dto.SeasonalWeatherDTO;
import kopo.poly.service.ISeasonalService;
import kopo.poly.util.KmaGridUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonalService implements ISeasonalService {

    private final WebClient           webClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper        objectMapper;

    @Value("${kma.ultra-srt-ncst.base-url}")
    private String kmaBaseUrl;

    @Value("${kma.ultra-srt-ncst.api-key}")
    private String kmaApiKey;           // 공공데이터포털 키 (Decoding·Encoding 모두 가능)

    @Value("${nongsaro.base-url}")
    private String nongsaroBaseUrl;

    @Value("${nongsaro.api-key}")
    private String nongsaroApiKey;

    /** 외부 API 단일 호출 타임아웃 */
    private static final Duration API_TIMEOUT = Duration.ofSeconds(5);

    /** 날씨 Redis 캐시 — 같은 격자·발표 시각 재호출 방지 (기상청 일일 트래픽 보호) */
    private static final String   WEATHER_CACHE_PREFIX = "seasonal:weather:";
    private static final Duration WEATHER_CACHE_TTL    = Duration.ofMinutes(10);

    /** 농사로 이미지 호스트 — rtnFileCours + rtnStreFileNm 앞에 붙임 */
    private static final String NONGSARO_HOST = "https://www.nongsaro.go.kr";

    /**
     * 농사로 이달의 음식 데이터가 있는 가장 오래된 연도
     * 실제 데이터는 2015~2019년에 집중 (2020년 이후는 거의 비어 있음, 2026-10 실측)
     * → 요청 연도부터 이 연도까지 목록을 모두 모아 이름 기준으로 합침
     */
    private static final int NONGSARO_FIRST_YEAR = 2015;

    /** 한 달 카드 최대 개수 */
    private static final int MAX_FOODS = 12;

    /** 앞면 소개 문구 최대 길이 */
    private static final int INTRO_MAX_LEN = 140;

    /*
     * 농사로 monthFd 응답 태그 (2026-10 실제 응답으로 확인)
     *  목록 monthFdmtLst : cntntsNo, fdmtNm, rtnFileCours, rtnStreFileNm, rtnThumbFileNm
     *  상세 monthFdmtDtl : ctvtIndcDtl(재배·유래), prchCheatDtl(■품종특성 ■구입요령),
     *                     cstdyMthDtl(■보관법 ■손질법), ntrIrdntEfcyDtl(영양·효능),
     *                     ntkMthDtl(섭취방법), cnsmpqyDtl(소비량), etcInfoDtl, rltRsrchInfoDtl
     */

    /**
     * 이름 키워드 → 분류 (농사로 응답에 분류 정보가 없어 서버에서 판단, 나머지는 vegetable)
     * 순서대로 검사 — 한 글자 키워드는 이름 전체 일치만 인정 ("배" ≠ "배추", "감" ≠ "감자")
     */
    private static final List<Map.Entry<String, List<String>>> CATEGORY_KEYWORDS = List.of(
            Map.entry("processed", List.of("고기", "소시지", "햄", "달걀", "계란", "닭", "닭가슴", "우유", "치즈", "요거트", "버터",
                                           "두부", "만두", "당면", "가래떡", "떡", "생햄", "장아찌", "김치", "동치미", "묵")),
            Map.entry("mushroom", List.of("버섯", "새송이", "느타리", "송이")),
            Map.entry("seafood",  List.of("새우", "대하", "꽃게", "게", "굴", "조개", "바지락", "전복", "오징어", "낙지", "문어",
                                          "고등어", "갈치", "꽁치", "전어", "삼치", "대구", "명태", "광어", "우럭", "멸치",
                                          "김", "미역", "다시마", "톳", "꼬막", "홍합", "멍게", "해삼", "주꾸미", "도미",
                                          "숭어", "조기", "가자미", "과메기", "방어", "새조개")),
            Map.entry("fruit",    List.of("사과", "배", "감", "곶감", "귤", "포도", "딸기", "복숭아", "자두", "참외", "수박",
                                          "매실", "유자", "한라봉", "키위", "참다래", "블루베리", "오디", "앵두", "살구",
                                          "무화과", "석류", "대추", "모과", "체리", "멜론", "레몬", "자몽", "건과일")),
            Map.entry("grain",    List.of("밤", "잣", "호두", "땅콩", "은행", "쌀", "찹쌀", "현미", "흑미", "보리", "밀", "우리밀", "오곡",
                                          "검은콩", "강낭콩", "풋콩", "대두", "녹두", "동부", "팥", "옥수수", "메밀", "귀리", "조", "수수", "기장",
                                          "율무", "참깨", "들깨", "견과", "도토리"))
    );

    // ── 날씨 ────────────────────────────────────────────────────────────────

    @Override
    public SeasonalWeatherDTO getCurrentWeather(double lat, double lon) throws Exception {

        int[]    grid = KmaGridUtil.toGrid(lat, lon);
        String[] base = KmaGridUtil.ultraSrtNcstBase();
        int nx = grid[0], ny = grid[1];
        String baseDate = base[0], baseTime = base[1];

        log.info("{}.getCurrentWeather Start | lat={} lon={} nx={} ny={} base={}{}",
                getClass().getName(), lat, lon, nx, ny, baseDate, baseTime);

        // ── 캐시 확인 ──
        String cacheKey = WEATHER_CACHE_PREFIX + nx + "_" + ny + "_" + baseDate + baseTime;
        String cached   = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.info("{}.getCurrentWeather 캐시 HIT | key={}", getClass().getName(), cacheKey);
            return objectMapper.readValue(cached, SeasonalWeatherDTO.class);
        }

        // ── 기상청 호출 — 키를 직접 인코딩 후 URI.create 로 이중 인코딩 방지 ──
        // Encoding 키(이미 %2B 등 포함)가 들어와도 동작하도록 '%' 가 있으면 그대로 사용
        String encodedKey = kmaApiKey.contains("%")
                ? kmaApiKey
                : URLEncoder.encode(kmaApiKey, StandardCharsets.UTF_8);
        String apiUrl = String.format(
                "%s?serviceKey=%s&dataType=JSON&numOfRows=10&pageNo=1&base_date=%s&base_time=%s&nx=%d&ny=%d",
                kmaBaseUrl, encodedKey, baseDate, baseTime, nx, ny);

        String body = webClient.get()
                .uri(URI.create(apiUrl))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();

        // 게이트웨이 오류 등은 dataType=JSON 이어도 XML 로 응답될 수 있음
        if (body == null || body.isBlank() || body.trim().startsWith("<")) {
            log.warn("기상청 응답 오류 | body={}", body);
            throw new RuntimeException("기상청 초단기실황 응답 오류");
        }

        JsonNode json = objectMapper.readTree(body);

        // 인증키 오류는 OpenAPI_ServiceResponse 형식 (예: SERVICE_KEY_IS_NOT_REGISTERED_ERROR)
        JsonNode authErr = json.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
        if (!authErr.isMissingNode()) {
            log.warn("기상청 인증 오류 | {} ({})", authErr.path("errMsg").asText(), authErr.path("returnAuthMsg").asText());
            throw new RuntimeException("기상청 인증 오류 " + authErr.path("errMsg").asText());
        }

        JsonNode root       = json.path("response");
        String   resultCode = root.path("header").path("resultCode").asText();
        if (!"00".equals(resultCode)) {
            log.warn("기상청 resultCode={} msg={}", resultCode, root.path("header").path("resultMsg").asText());
            throw new RuntimeException("기상청 초단기실황 조회 실패 resultCode=" + resultCode);
        }

        // category → obsrValue
        Map<String, String> values = new HashMap<>();
        for (JsonNode item : root.path("body").path("items").path("item")) {
            values.put(item.path("category").asText(), item.path("obsrValue").asText());
        }
        log.info("기상청 초단기실황 | {}", values);

        SeasonalWeatherDTO dto = SeasonalWeatherDTO.builder()
                .temperature (parseDouble(values.get("T1H")))
                .humidity    (toInt(parseDouble(values.get("REH"))))
                .rn1         (parseDouble(values.get("RN1")))
                .pty         (toInt(parseDouble(values.get("PTY"))))
                .windSpeed   (parseDouble(values.get("WSD")))
                .baseDateTime(baseDate.substring(0, 4) + "-" + baseDate.substring(4, 6) + "-" + baseDate.substring(6)
                              + "T" + baseTime.substring(0, 2) + ":" + baseTime.substring(2))
                .nx(nx)
                .ny(ny)
                .build();

        stringRedisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dto), WEATHER_CACHE_TTL);

        log.info("{}.getCurrentWeather End | temp={} humidity={}", getClass().getName(), dto.temperature(), dto.humidity());
        return dto;
    }

    /** "12.3" → 12.3, "강수없음" 등 숫자가 아닌 값 → 0.0 */
    private Double parseDouble(String v) {
        if (v == null) return null;
        try {
            return Double.parseDouble(v.replaceAll("[^0-9.\\-]", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer toInt(Double v) {
        return v == null ? null : (int) Math.round(v);
    }

    // ── 제철 식재료 ─────────────────────────────────────────────────────────

    /**
     * 연·월을 Redis 캐시 키로 사용 (CacheConfig 기본 TTL 3시간)
     * 빈 결과(인증키 미승인·API 장애)는 캐시하지 않음
     *
     * ⚠️ 반환은 반드시 ArrayList — Stream.toList() 의 불변 리스트는 final 클래스라
     *    CacheConfig(NON_FINAL 타입 정보)가 리스트 타입을 저장하지 않아 캐시 HIT 시 역직렬화 실패
     * 캐시 이름 seasonalFoodList — 이전 이름(seasonalFoods)에 저장된 깨진 값을 읽지 않도록 변경
     */
    @Cacheable(value = "seasonalFoodList", key = "#year + '_' + #month", unless = "#result.isEmpty()")
    @Override
    public List<SeasonalFoodDTO> getSeasonalFoods(int year, int month) throws Exception {

        log.info("{}.getSeasonalFoods Start | year={} month={}", getClass().getName(), year, month);

        // 요청 연도 ~ NONGSARO_FIRST_YEAR 목록 병렬 조회 (최신 연도 우선, 이름 중복 제거)
        List<Integer> years = new ArrayList<>();
        for (int y = year; y >= NONGSARO_FIRST_YEAR; y--) years.add(y);

        List<List<Map<String, String>>> byYear = years.parallelStream()
                .map(y -> fetchNongsaroList(y, month))
                .toList();   // 순서 유지 → 최신 연도 먼저

        Map<String, Map<String, String>> unique = new LinkedHashMap<>();
        byYear.forEach(list -> list.forEach(item -> {
            String name = item.get("fdmtNm");
            if (name != null && !name.isBlank()) unique.putIfAbsent(name.trim(), item);
        }));
        List<Map<String, String>> items = unique.values().stream().limit(MAX_FOODS).toList();
        log.info("농사로 {}월 목록 | {}건 {}", month, items.size(), unique.keySet());

        // 식재료별 상세 병렬 조회 — 상세 실패 시 목록 정보만으로 카드 생성
        List<SeasonalFoodDTO> result = items.parallelStream()
                .map(item -> {
                    Map<String, String> merged = new HashMap<>(item);
                    String cntntsNo = item.get("cntntsNo");
                    if (cntntsNo != null) merged.putAll(fetchNongsaroDetail(cntntsNo));
                    return toFoodDTO(merged, month);
                })
                .filter(dto -> dto.name() != null)
                .collect(Collectors.toCollection(ArrayList::new));   // 캐시 역직렬화용 — toList() 금지

        log.info("{}.getSeasonalFoods End | count={}", getClass().getName(), result.size());
        return result;
    }

    /** 농사로 monthFdmtLst — 해당 연·월 식재료 목록, 실패 시 빈 목록 */
    private List<Map<String, String>> fetchNongsaroList(int year, int month) {
        try {
            String url = String.format("%s/monthFdmtLst?apiKey=%s&thisYear=%d&thisMonth=%02d",
                    nongsaroBaseUrl, URLEncoder.encode(nongsaroApiKey, StandardCharsets.UTF_8), year, month);
            return parseNongsaroItems(callNongsaro(url));

        } catch (Exception e) {
            log.warn("농사로 목록 조회 실패 | {}-{} error={}", year, month, e.getMessage());
            return List.of();
        }
    }

    /** 농사로 monthFdmtDtl — 식재료 상세, 실패 시 빈 Map */
    private Map<String, String> fetchNongsaroDetail(String cntntsNo) {
        try {
            String url = String.format("%s/monthFdmtDtl?apiKey=%s&cntntsNo=%s",
                    nongsaroBaseUrl, URLEncoder.encode(nongsaroApiKey, StandardCharsets.UTF_8),
                    URLEncoder.encode(cntntsNo, StandardCharsets.UTF_8));
            List<Map<String, String>> items = parseNongsaroItems(callNongsaro(url));
            return items.isEmpty() ? Map.of() : items.get(0);

        } catch (WebClientResponseException e) {
            log.warn("농사로 상세 조회 실패 | cntntsNo={} status={}", cntntsNo, e.getStatusCode());
            return Map.of();
        } catch (Exception e) {
            log.warn("농사로 상세 조회 실패 | cntntsNo={} error={}", cntntsNo, e.getMessage());
            return Map.of();
        }
    }

    private String callNongsaro(String url) {
        return webClient.get()
                .uri(URI.create(url))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(API_TIMEOUT)
                .block();
    }

    /**
     * 농사로 XML → item 별 {태그명: 텍스트} 목록
     * resultCode 가 00 이 아니면 RuntimeException
     */
    private List<Map<String, String>> parseNongsaroItems(String xml) throws Exception {
        if (xml == null || xml.isBlank()) throw new RuntimeException("농사로 응답 없음");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);   // XXE 방지
        Document doc = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        String resultCode = textOf(doc.getDocumentElement(), "resultCode");
        if (resultCode != null && !"00".equals(resultCode)) {
            log.warn("농사로 resultCode={} msg={}", resultCode, textOf(doc.getDocumentElement(), "resultMsg"));
            throw new RuntimeException("농사로 조회 실패 resultCode=" + resultCode);
        }

        NodeList itemNodes = doc.getElementsByTagName("item");
        List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < itemNodes.getLength(); i++) {
            Map<String, String> fields = new LinkedHashMap<>();
            NodeList children = itemNodes.item(i).getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node c = children.item(j);
                if (c.getNodeType() == Node.ELEMENT_NODE) fields.put(c.getNodeName(), c.getTextContent().trim());
            }
            items.add(fields);
        }

        if (!items.isEmpty()) log.debug("농사로 태그 | {}", items.get(0).keySet());
        return items;
    }

    private String textOf(Element root, String tag) {
        NodeList nl = root.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent().trim() : null;
    }

    /** 농사로 태그 Map → 카드 DTO */
    private SeasonalFoodDTO toFoodDTO(Map<String, String> f, int month) {
        String name     = text(f, "fdmtNm");
        String purchase = text(f, "prchCheatDtl");    // ■ 품종특성 ■ 구입요령
        String custody  = text(f, "cstdyMthDtl");     // ■ 보관법 ■ 손질법

        return SeasonalFoodDTO.builder()
                .cntntsNo (f.get("cntntsNo"))
                .name     (name)
                .category (classify(name))
                .month    (month)
                .imageUrl (toImageUrl(f.get("rtnFileCours"), f.get("rtnStreFileNm")))
                .intro    (toIntro(text(f, "ctvtIndcDtl")))
                .buyTip   (sectionOr(purchase, "구입요령"))
                .storage  (sectionOr(custody, "보관법"))
                .prep     (section(custody, "손질법"))
                .nutrition(bullets(text(f, "ntrIrdntEfcyDtl")))
                .build();
    }

    /** 태그 값 (HTML 제거), 없거나 비어 있으면 null */
    private String text(Map<String, String> f, String tag) {
        String v = f.get(tag);
        if (v == null) return null;
        v = stripHtml(v);
        return v.isBlank() ? null : v;
    }

    /**
     * "■ 제목\n- 내용 ■ 제목2 ..." 형식에서 heading 으로 시작하는 단락의 본문만 추출
     * 없으면 null
     */
    private String section(String text, String heading) {
        if (text == null) return null;
        for (String part : text.split("■")) {
            String p = part.trim();
            if (p.startsWith(heading)) {
                String body = bullets(p.substring(heading.length()));
                return body == null || body.isBlank() ? null : body;
            }
        }
        return null;
    }

    /** section 이 없으면 전체 본문 (소제목 형식이 아닌 응답 대비) */
    private String sectionOr(String text, String heading) {
        String s = section(text, heading);
        return s != null ? s : bullets(text);
    }

    /** "■" 글머리 정리 → 줄 단위 "· " 목록 (프론트는 white-space: pre-line 으로 표시) */
    private String bullets(String text) {
        if (text == null) return null;
        String out = text.replace("■", "\n")
                .lines()
                .map(String::trim)
                .map(l -> l.replaceFirst("^-\\s*", "· "))
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining("\n"));
        return out.isBlank() ? null : out;
    }

    /** 재배·유래 첫 단락을 앞면 소개 문구로 (길면 문장 단위로 자름) */
    private String toIntro(String text) {
        if (text == null) return null;
        String first = Arrays.stream(text.split("■"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse(null);
        if (first == null) return null;
        first = first.replaceAll("\\s+", " ");
        if (first.length() <= INTRO_MAX_LEN) return first;

        String cut = first.substring(0, INTRO_MAX_LEN);
        int dot = cut.lastIndexOf(". ");
        return dot > 40 ? cut.substring(0, dot + 1) : cut + "…";
    }

    /** 농사로 본문의 HTML 태그·엔티티 제거 */
    private String stripHtml(String html) {
        return html.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("<[^>]*>", "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    /**
     * 이미지 URL = https://www.nongsaro.go.kr/{rtnFileCours}/{rtnStreFileNm}
     * 예) https://www.nongsaro.go.kr/cms_contents/854/249155_MF_BIMG_01.jpg (https 응답 확인)
     * 여러 장이면 "|" 구분 — 첫 장만 사용
     */
    private String toImageUrl(String cours, String fileNm) {
        if (cours == null || fileNm == null) return null;
        String c = cours.split("\\|")[0].trim().replaceAll("^/+|/+$", "");
        String f = fileNm.split("\\|")[0].trim();
        if (c.isEmpty() || f.isEmpty()) return null;
        return NONGSARO_HOST + "/" + c + "/" + f;
    }

    /** 식재료명 → 분류 */
    private String classify(String name) {
        if (name == null) return "vegetable";
        String n = name.trim();
        for (Map.Entry<String, List<String>> e : CATEGORY_KEYWORDS) {
            for (String kw : e.getValue()) {
                boolean hit = kw.length() == 1 ? n.equals(kw) : n.contains(kw);
                if (hit) return e.getKey();
            }
        }
        return "vegetable";
    }
}
