package kopo.poly.dto;

import java.util.List;

/**
 * 재료 1건에 대한 네이버 쇼핑 검색 결과 묶음
 *
 * <p>results 에는 정렬 기준별 최대 3개 항목(기본·최저가·최고가)이 포함됩니다.</p>
 */
public record IngredientSearchDTO(
        String ingredient,
        List<ShoppingDTO> results
) {}
