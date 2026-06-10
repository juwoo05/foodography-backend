package kopo.poly.controller;

import kopo.poly.dto.IngredientSearchDTO;
import kopo.poly.dto.ShoppingDTO;
import kopo.poly.service.IShoppingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shopping")
@RequiredArgsConstructor
public class ShoppingController {

    private final IShoppingService shoppingService;

    /**
     * 네이버 쇼핑 검색 — 최신 레시피의 ADDITIONAL_INGREDIENTS 기준
     *
     * <p>재료별로 기본·최저가·최고가 정렬 결과 각 1건씩(최대 3건) 반환합니다.</p>
     * GET /api/shopping/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<IngredientSearchDTO>> search(
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.search Start | userId={}", getClass().getName(), userId);

        try {
            List<IngredientSearchDTO> result = shoppingService.searchAdditionalIngredients(userId);
            log.info("{}.search End | groups={}", getClass().getName(), result.size());
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.warn("{}.search 비즈니스 오류 | {}", getClass().getName(), e.getMessage());
            return ResponseEntity.ok(List.of());   // 레시피 없음 등 → 빈 목록 반환

        } catch (Exception e) {
            log.error("{}.search 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 장바구니 저장 (MYCART INSERT)
     * POST /api/shopping/cart
     */
    @PostMapping("/cart")
    public ResponseEntity<ShoppingDTO> addToCart(
            @RequestBody ShoppingDTO dto,
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.addToCart | userId={} ingredient={}", getClass().getName(), userId, dto.ingredient());

        try {
            ShoppingDTO saved = shoppingService.addToCart(dto, userId);
            return ResponseEntity.ok(saved);

        } catch (Exception e) {
            log.error("{}.addToCart 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 장바구니 조회
     * GET /api/shopping/cart
     */
    @GetMapping("/cart")
    public ResponseEntity<List<ShoppingDTO>> getCart(
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());

        try {
            List<ShoppingDTO> result = shoppingService.getCart(userId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("{}.getCart 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 장바구니 단일 항목 삭제
     * DELETE /api/shopping/cart/{cartId}
     */
    @DeleteMapping("/cart/{cartId}")
    public ResponseEntity<Void> removeFromCart(
            @PathVariable Integer cartId,
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.removeFromCart | cartId={} userId={}", getClass().getName(), cartId, userId);

        try {
            shoppingService.removeFromCart(cartId, userId);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.warn("{}.removeFromCart 항목 없음 | {}", getClass().getName(), e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("{}.removeFromCart 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 장바구니 전체 비우기
     * DELETE /api/shopping/cart
     */
    @DeleteMapping("/cart")
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.clearCart | userId={}", getClass().getName(), userId);

        try {
            shoppingService.clearCart(userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("{}.clearCart 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
