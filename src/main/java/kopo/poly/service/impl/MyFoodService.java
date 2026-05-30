package kopo.poly.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kopo.poly.dto.FoodDbDTO;
import org.springframework.cache.annotation.Cacheable;
import kopo.poly.repository.FoodNutritionRepository;
import kopo.poly.repository.entity.FoodNutritionEntity;
import kopo.poly.service.IMyFoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MyFoodService implements IMyFoodService {

    private final VectorStore                vectorStore;           // Spring AI — PineconeVectorStore
    private final ChatClient.Builder         chatClientBuilder;     // Spring AI — auto-configured
    private final WebClient                  webClient;             // 식품안전처 API 호출용
    private final ObjectMapper               objectMapper;          // Spring Boot auto-configured
    private final FoodNutritionRepository foodNutritionRepo;     // MariaDB — 영양 데이터

    /** 요청마다 새로 빌드하지 않도록 서비스 초기화 시 1회 생성 */
    private ChatClient chatClient;

    @PostConstruct
    private void init() {
        this.chatClient = chatClientBuilder.build();
    }

    @Value("${food-safety.api-key}")
    private String foodSafetyApiKey;

    @Value("${food-safety.base-url}")
    private String foodSafetyBaseUrl;

    /** 공공데이터포털 API 한 페이지당 항목 수 — 최대 500 제한 */
    private static final int PAGE_SIZE = 500;

    /** VectorStore 배치 업서트 크기 — Pinecone 권장 100건 단위 */
    private static final int BATCH_SIZE = 100;

    /** Pinecone 유사도 검색 상위 K */
    private static final int TOP_K = 5;

    /**
     * Claude 매칭 프롬프트 템플릿.
     * 영양 데이터는 MariaDB 에서 조회하므로 Claude 에게는 식품명·코드만 반환하도록 요청.
     */
    private static final String MATCH_PROMPT_TEMPLATE = """
            사용자 식재료명: %s

            아래는 Pinecone 유사도 검색 결과입니다 (상위 %d개):
            %s
            반드시 위 후보 중 사용자 식재료 "%s" 와 가장 잘 일치하는 식품 1개를 골라
            아래 JSON 형식으로만 응답하세요 (다른 텍스트, 마크다운, 설명 전부 금지):
            {"foodCd":"식품코드","foodNm":"식품명"}
            """;

    // ── ① 배치 인덱싱 (스케줄러 호출) ───────────────────────────────────────

    @Override
    public void indexFoodDatabase() throws Exception {

        log.info("{}.indexFoodDatabase Start!", this.getClass().getName());

        String encodedKey = URLEncoder.encode(foodSafetyApiKey, StandardCharsets.UTF_8);

        int pageNo     = 1;
        int totalCount = Integer.MAX_VALUE;
        int indexed    = 0;

        while ((long) (pageNo - 1) * PAGE_SIZE < totalCount) {

            // ── STEP 1: 공공데이터포털 식품영양성분 API 호출 ──
            String apiUrl = String.format(
                    "%s?serviceKey=%s&pageNo=%d&numOfRows=%d&type=json",
                    foodSafetyBaseUrl, encodedKey, pageNo, PAGE_SIZE);

            log.info("{}.indexFoodDatabase API 호출 pageNo={}", this.getClass().getName(), pageNo);

            Map<?, ?> responseMap;
            try {
                responseMap = webClient.get()
                        .uri(URI.create(apiUrl))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
            } catch (Exception e) {
                log.error("{}.indexFoodDatabase API 호출 실패 pageNo={} | {}",
                        this.getClass().getName(), pageNo, e.getMessage());
                break;
            }

            // ── STEP 2: 응답 구조 파싱 — body.items.item ──
            if (responseMap == null || !responseMap.containsKey("body")) {
                log.warn("{}.indexFoodDatabase 응답 없음 pageNo={} | responseKeys={}",
                        this.getClass().getName(), pageNo,
                        responseMap != null ? responseMap.keySet() : "null");
                break;
            }

            Map<?, ?> header = (Map<?, ?>) responseMap.get("header");
            if (header != null) {
                Object codeObj = header.get("resultCode");
                String resultCode = codeObj != null ? codeObj.toString() : "";
                if (!"00".equals(resultCode)) {
                    Object msgObj = header.get("resultMsg");
                    log.error("{}.indexFoodDatabase API 오류 pageNo={} | resultCode={} resultMsg={}",
                            this.getClass().getName(), pageNo, resultCode,
                            msgObj != null ? msgObj.toString() : "");
                    break;
                }
            }

            Map<?, ?> body = (Map<?, ?>) responseMap.get("body");

            if (pageNo == 1) {
                Object totalObj = body.get("totalCount");
                totalCount = (totalObj != null)
                        ? Integer.parseInt(totalObj.toString().replaceAll(",", ""))
                        : 0;
                log.info("{}.indexFoodDatabase 전체 건수={}", this.getClass().getName(), totalCount);
            }

            List<?> rows = extractItems(body);
            if (rows.isEmpty()) {
                log.info("{}.indexFoodDatabase 더 이상 데이터 없음 pageNo={}",
                        this.getClass().getName(), pageNo);
                break;
            }

            // ── STEP 3: Document(Pinecone) + Entity(MariaDB) 동시 구성 ──
            // Pinecone : food_nm(임베딩 텍스트) + food_cd, food_nm(메타데이터)
            // MariaDB  : food_cd, food_nm, kcal, protein, fat, carbs
            List<Document>             docs     = new ArrayList<>();
            List<FoodNutritionEntity>  entities = new ArrayList<>();

            for (Object rowObj : rows) {
                if (!(rowObj instanceof Map<?, ?> row)) continue;

                String foodNm = nullSafe(row, "FOOD_NM_KR");
                if (foodNm == null || foodNm.isBlank()) continue;

                String foodCd = nullSafe(row, "FOOD_CD");

                // Pinecone — 유사도 검색에 필요한 필드만 저장
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("food_cd", foodCd != null ? foodCd : "");
                metadata.put("food_nm", foodNm);
                docs.add(new Document(foodNm, metadata));

                // MariaDB — 영양 데이터 저장 (food_cd 없는 항목 제외)
                if (foodCd != null && !foodCd.isBlank()) {
                    entities.add(FoodNutritionEntity.builder()
                            .foodCd(foodCd)
                            .foodNm(foodNm)
                            .kcal(toDouble(row, "AMT_NUM1"))    // 에너지(kcal)
                            .protein(toDouble(row, "AMT_NUM3")) // 단백질(g)
                            .fat(toDouble(row, "AMT_NUM4"))     // 지방(g)
                            .carbs(toDouble(row, "AMT_NUM6"))   // 탄수화물(g)
                            .build());
                }
            }

            // ── STEP 4: Pinecone 배치 업서트 ──
            for (int i = 0; i < docs.size(); i += BATCH_SIZE) {
                List<Document> batch = docs.subList(i, Math.min(i + BATCH_SIZE, docs.size()));
                vectorStore.add(batch);
            }

            // ── STEP 5: MariaDB 배치 저장 ──
            if (!entities.isEmpty()) {
                foodNutritionRepo.saveAll(entities);
            }

            indexed += docs.size();
            pageNo++;

            log.info("{}.indexFoodDatabase 진행 indexed={} / total={}",
                    this.getClass().getName(), indexed, totalCount);
        }

        log.info("{}.indexFoodDatabase End! totalIndexed={}", this.getClass().getName(), indexed);
    }

    // ── ② 유사도 검색 + Claude 매칭 (컨트롤러 호출) ─────────────────────────

    /**
     * scanId 를 Redis 캐시 키로 사용.
     * 동일 scanId 재요청 시 Pinecone · Claude 호출 없이 캐시에서 즉시 반환.
     */
    @Cacheable(value = "foodMatch", key = "#scanId")
    @Override
    public List<FoodDbDTO> matchIngredients(String scanId, List<String> ingredients) throws Exception {

        log.info("{}.matchIngredients Start! scanId={} ingredientCount={}",
                this.getClass().getName(), scanId,
                ingredients != null ? ingredients.size() : 0);

        List<FoodDbDTO> results = new ArrayList<>();

        for (String ingredient : ingredients) {

            log.info("{}.matchIngredients 검색 시작 ingredient={}",
                    this.getClass().getName(), ingredient);

            // ── STEP 1: Pinecone 유사도 검색 ──
            List<Document> candidates = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(ingredient)
                            .topK(TOP_K)
                            .build()
            );

            if (candidates == null || candidates.isEmpty()) {
                log.warn("{}.matchIngredients 후보 없음 ingredient={}",
                        this.getClass().getName(), ingredient);
                results.add(FoodDbDTO.builder()
                        .scanId(scanId)
                        .ingredient(ingredient)
                        .build());
                continue;
            }

            // ── STEP 2: 후보 목록 구성 (식품명 + 코드만) ──
            StringBuilder candidateStr = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                Map<String, Object> meta = candidates.get(i).getMetadata();
                candidateStr.append(String.format(
                        "%d. %s (코드: %s)%n",
                        i + 1,
                        meta.getOrDefault("food_nm", ""),
                        meta.getOrDefault("food_cd", "")
                ));
            }

            // ── STEP 3: Claude 매칭 요청 ──
            String prompt = String.format(
                    MATCH_PROMPT_TEMPLATE,
                    ingredient, candidates.size(), candidateStr, ingredient);

            String rawResponse;
            try {
                rawResponse = chatClient.prompt(prompt).call().content();
            } catch (Exception e) {
                log.error("{}.matchIngredients Claude 호출 실패 ingredient={} | {}",
                        this.getClass().getName(), ingredient, e.getMessage());
                results.add(FoodDbDTO.builder()
                        .scanId(scanId)
                        .ingredient(ingredient)
                        .build());
                continue;
            }

            log.info("{}.matchIngredients Claude 응답 ingredient={} rawResponse={}",
                    this.getClass().getName(), ingredient, rawResponse);

            // ── STEP 4: Claude 응답 파싱 → food_cd 추출 ──
            String[] parsed = parseClaudeResponse(rawResponse);
            String matchedFoodCd = parsed[0];
            String matchedFoodNm = parsed[1];

            // ── STEP 5: MariaDB 에서 영양 데이터 조회 ──
            FoodDbDTO matched = lookupNutrition(scanId, ingredient, matchedFoodCd, matchedFoodNm);
            results.add(matched);

            log.info("{}.matchIngredients 매칭 완료 | ingredient={} | matchedFoodNm={} | kcal={}",
                    this.getClass().getName(),
                    ingredient,
                    matched.matchedFoodNm(),
                    matched.matchedKcal());
        }

        log.info("{}.matchIngredients End! resultCount={}", this.getClass().getName(), results.size());

        return results;
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    /**
     * Claude 응답에서 foodCd, foodNm 을 추출합니다.
     *
     * @return String[]{foodCd, foodNm} — 파싱 실패 시 빈 문자열
     */
    private String[] parseClaudeResponse(String rawResponse) {
        try {
            String json  = rawResponse.trim();
            int    start = json.indexOf('{');
            int    end   = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            return new String[]{
                    node.path("foodCd").asText(""),
                    node.path("foodNm").asText("")
            };
        } catch (Exception e) {
            log.warn("{}.parseClaudeResponse JSON 파싱 실패 | {}",
                    this.getClass().getName(), e.getMessage());
            return new String[]{"", ""};
        }
    }

    /**
     * MariaDB 에서 영양 데이터를 조회하여 {@link FoodDbDTO} 를 반환합니다.
     *
     * <ol>
     *   <li>foodCd 로 조회</li>
     *   <li>없으면 foodNm 으로 폴백</li>
     *   <li>그래도 없으면 영양값 0 으로 반환</li>
     * </ol>
     */
    private FoodDbDTO lookupNutrition(String scanId, String ingredient,
                                       String foodCd, String foodNm) {
        FoodNutritionEntity entity = null;

        if (foodCd != null && !foodCd.isBlank()) {
            entity = foodNutritionRepo.findById(foodCd).orElse(null);
        }
        if (entity == null && foodNm != null && !foodNm.isBlank()) {
            entity = foodNutritionRepo.findByFoodNm(foodNm).orElse(null);
        }

        if (entity == null) {
            log.warn("{}.lookupNutrition 영양 데이터 없음 foodCd={} foodNm={}",
                    this.getClass().getName(), foodCd, foodNm);
            return FoodDbDTO.builder()
                    .scanId(scanId)
                    .ingredient(ingredient)
                    .matchedFoodNm(foodNm)
                    .matchedKcal(0.0)
                    .matchedProtein(0.0)
                    .matchedFat(0.0)
                    .matchedCarbs(0.0)
                    .build();
        }

        return FoodDbDTO.builder()
                .scanId(scanId)
                .ingredient(ingredient)
                .matchedFoodNm(entity.getFoodNm())
                .matchedKcal(entity.getKcal())
                .matchedProtein(entity.getProtein())
                .matchedFat(entity.getFat())
                .matchedCarbs(entity.getCarbs())
                .build();
    }

    /**
     * 공공데이터포털 응답 body 에서 item 목록을 추출합니다.
     * 단건 응답 시 item 이 List 가 아닌 Map 으로 오는 경우를 방어합니다.
     */
    @SuppressWarnings("unchecked")
    private List<?> extractItems(Map<?, ?> body) {
        Object itemsObj = body.get("items");
        if (itemsObj == null) return List.of();

        if (itemsObj instanceof Map<?, ?> itemsMap) {
            Object itemObj = itemsMap.get("item");
            if (itemObj instanceof List)  return (List<?>) itemObj;
            if (itemObj instanceof Map)   return List.of(itemObj);
        }
        if (itemsObj instanceof List) return (List<?>) itemsObj;

        return List.of();
    }

    /** Map 에서 String 값 안전 추출 (null 허용) */
    private String nullSafe(Map<?, ?> row, String key) {
        Object val = row.get(key);
        return (val != null) ? val.toString().trim() : null;
    }

    /**
     * Map 에서 Double 값 안전 추출.
     * null, 빈 문자열, "-" 는 0.0 으로 처리합니다.
     */
    private double toDouble(Map<?, ?> row, String key) {
        Object val = row.get(key);
        if (val == null) return 0.0;
        String str = val.toString().trim();
        if (str.isEmpty() || str.equals("-")) return 0.0;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
