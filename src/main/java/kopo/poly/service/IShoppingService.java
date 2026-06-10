package kopo.poly.service;

import kopo.poly.dto.IngredientSearchDTO;
import kopo.poly.dto.ShoppingDTO;

import java.util.List;

public interface IShoppingService {

    /**
     * JWT 인증된 userId 기준 가장 최신 RECIPE 행의 ADDITIONAL_INGREDIENTS 를 조회하고
     * 각 재료에 대해 네이버 쇼핑 API 를 3종(기본·최저가·최고가) 정렬로 검색하여 반환합니다.
     *
     * @param userId JWT Subject 에서 추출한 사용자 ID
     */
    List<IngredientSearchDTO> searchAdditionalIngredients(Integer userId) throws Exception;

    /**
     * 장바구니 항목 저장 (MYCART INSERT)
     *
     * @return cartId 가 채워진 저장 결과 DTO
     */
    ShoppingDTO addToCart(ShoppingDTO dto, Integer userId) throws Exception;

    /** 사용자 장바구니 전체 조회 */
    List<ShoppingDTO> getCart(Integer userId) throws Exception;

    /** 단일 항목 삭제 — 소유권 검증 포함 */
    void removeFromCart(Integer cartId, Integer userId) throws Exception;

    /** 장바구니 전체 비우기 */
    void clearCart(Integer userId) throws Exception;
}
