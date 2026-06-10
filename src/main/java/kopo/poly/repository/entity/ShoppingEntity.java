package kopo.poly.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

/**
 * MYCART 테이블 엔티티 — 사용자 장바구니 항목
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@Entity
@Table(
        name = "MYCART",
        indexes = {
                @Index(name = "IDX_MYCART_USER", columnList = "USER_ID")
        }
)
public class ShoppingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CART_ID", updatable = false)
    private Integer cartId;

    /** FK → USER_INFO.USER_ID */
    @Column(name = "USER_ID", nullable = false)
    private Integer userId;

    /** FK → RECIPE.RECIPE_ID — 검색 기준 레시피 (nullable: 직접 추가 가능성 고려) */
    @Column(name = "RECIPE_ID")
    private Integer recipeId;

    /** 검색한 재료명 */
    @Column(name = "INGREDIENT", nullable = false, length = 100)
    private String ingredient;

    /** 정렬 기준 — "기본" | "최저가" | "최고가" */
    @Column(name = "SORT_TYPE", length = 20)
    private String sortType;

    /** 상품명 */
    @Column(name = "TITLE", nullable = false, length = 500)
    private String title;

    /** 상품 상세 링크 */
    @Column(name = "LINK", nullable = false, length = 500)
    private String link;

    /** 상품 대표 이미지 URL */
    @Column(name = "IMAGE", length = 500)
    private String image;

    /** 최저가 */
    @Column(name = "LPRICE", length = 20)
    private String lprice;

    /** 쇼핑몰명 */
    @Column(name = "MALL_NAME", length = 100)
    private String mallName;

    @Column(name = "REG_DT", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;
}
