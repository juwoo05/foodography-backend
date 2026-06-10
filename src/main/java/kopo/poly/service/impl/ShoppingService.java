package kopo.poly.service.impl;

import kopo.poly.dto.IngredientSearchDTO;
import kopo.poly.dto.ShoppingDTO;
import kopo.poly.repository.RecipeRepository;
import kopo.poly.repository.ShoppingRepository;
import kopo.poly.repository.entity.RecipeEntity;
import kopo.poly.repository.entity.ShoppingEntity;
import kopo.poly.service.IShoppingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingService implements IShoppingService {

    private final WebClient         webClient;
    private final RecipeRepository  recipeRepository;
    private final ShoppingRepository shoppingRepository;

    @Value("${naver.shopping.client-id}")
    private String naverClientId;

    @Value("${naver.shopping.client-secret}")
    private String naverClientSecret;

    /** 네이버 쇼핑 검색 API 엔드포인트 */
    private static final String NAVER_URL = "https://openapi.naver.com/v1/search/shop.json";

    /** 네이버 API 단일 호출 타임아웃 */
    private static final Duration NAVER_TIMEOUT = Duration.ofSeconds(5);

    // ── 검색 ────────────────────────────────────────────────────────────────

    @Override
    public List<IngredientSearchDTO> searchAdditionalIngredients(Integer userId) throws Exception {

        log.info("{}.searchAdditionalIngredients Start | userId={}", getClass().getName(), userId);
        log.info("[Naver] clientId={}... secret={}...",
                naverClientId  != null ? naverClientId.substring(0, Math.min(4, naverClientId.length()))   + "****" : "NULL",
                naverClientSecret != null ? naverClientSecret.substring(0, Math.min(4, naverClientSecret.length())) + "****" : "NULL");

        // 해당 유저의 가장 최신 RECIPE 행 조회
        RecipeEntity recipe = recipeRepository.findTopByUserIdOrderByRegDtDesc(userId)
                .orElseThrow(() -> new RuntimeException("분석된 레시피가 없습니다. 먼저 냉장고 분석을 진행해주세요."));

        List<String> additional = recipe.getAdditionalIngredients();
        Integer      recipeId   = recipe.getRecipeId();

        log.info("최신 레시피 조회 완료 | recipeId={} additionalCount={}",
                recipeId, additional != null ? additional.size() : 0);

        if (additional == null || additional.isEmpty()) {
            log.info("추가 재료 없음 — 빈 목록 반환");
            return List.of();
        }

        // 재료별 병렬 검색 — parallelStream 으로 JVM ForkJoinPool 활용
        List<IngredientSearchDTO> results = additional.parallelStream()
                .map(ingredient -> new IngredientSearchDTO(ingredient, searchNaverTop3(ingredient, recipeId)))
                .filter(dto -> !dto.results().isEmpty())
                .toList();

        log.info("{}.searchAdditionalIngredients End | resultGroups={}", getClass().getName(), results.size());
        return results;
    }

    /**
     * 네이버 쇼핑 API 단일 호출 — 관련도 순 상위 3개 반환
     *
     * @param ingredient 검색 재료명
     * @param recipeId   연결 레시피 ID
     * @return 상위 3개 상품 DTO 목록, 실패 시 빈 목록
     */
    @SuppressWarnings("unchecked")
    private List<ShoppingDTO> searchNaverTop3(String ingredient, Integer recipeId) {
        try {
            Map<String, Object> response = webClient.get()
                    .uri(NAVER_URL + "?query={q}&display=3&sort=sim", ingredient)
                    .header("X-Naver-Client-Id",     naverClientId)
                    .header("X-Naver-Client-Secret", naverClientSecret)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(NAVER_TIMEOUT)
                    .block();

            if (response == null || !response.containsKey("items")) return List.of();

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items == null || items.isEmpty()) return List.of();

            List<ShoppingDTO> result = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                result.add(ShoppingDTO.builder()
                        .ingredient(ingredient)
                        .sortType(String.valueOf(i + 1))   // "1", "2", "3" — 순위 번호
                        .title(stripHtml((String) item.get("title")))
                        .link((String)  item.get("link"))
                        .image((String) item.get("image"))
                        .lprice((String) item.get("lprice"))
                        .mallName((String) item.get("mallName"))
                        .recipeId(recipeId)
                        .build());
            }
            return result;

        } catch (WebClientResponseException e) {
            log.warn("네이버 쇼핑 검색 실패 | ingredient={} status={} body={}",
                    ingredient, e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.warn("네이버 쇼핑 검색 실패 | ingredient={} error={}", ingredient, e.getMessage());
            return List.of();
        }
    }

    /** Naver title 에서 HTML 태그(<b>, </b> 등) 제거 */
    private String stripHtml(String html) {
        return html == null ? "" : html.replaceAll("<[^>]*>", "");
    }

    // ── 장바구니 ────────────────────────────────────────────────────────────

    @Override
    public ShoppingDTO addToCart(ShoppingDTO dto, Integer userId) throws Exception {

        log.info("{}.addToCart | userId={} ingredient={} sortType={}",
                getClass().getName(), userId, dto.ingredient(), dto.sortType());

        ShoppingEntity entity = ShoppingEntity.builder()
                .userId    (userId)
                .recipeId  (dto.recipeId())
                .ingredient(dto.ingredient())
                .sortType  (dto.sortType())
                .title     (dto.title())
                .link      (dto.link())
                .image     (dto.image())
                .lprice    (dto.lprice())
                .mallName  (dto.mallName())
                .build();

        ShoppingEntity saved = shoppingRepository.save(entity);

        log.info("{}.addToCart 완료 | cartId={}", getClass().getName(), saved.getCartId());

        return toDTO(saved);
    }

    @Override
    public List<ShoppingDTO> getCart(Integer userId) throws Exception {
        return shoppingRepository.findByUserIdOrderByRegDtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public void removeFromCart(Integer cartId, Integer userId) throws Exception {
        ShoppingEntity entity = shoppingRepository.findByCartIdAndUserId(cartId, userId)
                .orElseThrow(() -> new RuntimeException("장바구니 항목을 찾을 수 없습니다. cartId=" + cartId));
        shoppingRepository.delete(entity);
        log.info("{}.removeFromCart | cartId={} userId={}", getClass().getName(), cartId, userId);
    }

    @Override
    @Transactional
    public void clearCart(Integer userId) throws Exception {
        shoppingRepository.deleteAllByUserId(userId);
        log.info("{}.clearCart | userId={}", getClass().getName(), userId);
    }

    // ── 내부 변환 헬퍼 ─────────────────────────────────────────────────────

    private ShoppingDTO toDTO(ShoppingEntity e) {
        return ShoppingDTO.builder()
                .cartId    (e.getCartId())
                .userId    (e.getUserId())
                .recipeId  (e.getRecipeId())
                .ingredient(e.getIngredient())
                .sortType  (e.getSortType())
                .title     (e.getTitle())
                .link      (e.getLink())
                .image     (e.getImage())
                .lprice    (e.getLprice())
                .mallName  (e.getMallName())
                .build();
    }
}
