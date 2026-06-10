package kopo.poly.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * REVIEW 테이블 JPA 엔티티
 * USER_INFO, RECIPE 와 FK 관계 — 즉시 로딩 오버헤드 방지를 위해 단순 컬럼으로만 유지
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@Table(
        name = "REVIEW",
        indexes = {
                @Index(name = "IDX_REVIEW_USER_ID",   columnList = "USER_ID"),
                @Index(name = "IDX_REVIEW_RECIPE_ID", columnList = "RECIPE_ID"),
                @Index(name = "IDX_REVIEW_REG_DT",    columnList = "REG_DT")
        }
)
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REVIEW_ID", updatable = false)
    private Integer reviewId;

    /** FK → USER_INFO.USER_ID */
    @Column(name = "USER_ID", nullable = false)
    private Integer userId;

    /** FK → RECIPE.RECIPE_ID */
    @Column(name = "RECIPE_ID", nullable = false)
    private Integer recipeId;

    /** 평점 1 ~ 5 */
    @Column(name = "RATING", nullable = false)
    private Integer rating;

    /** 리뷰 내용 */
    @Column(name = "COMMENT", columnDefinition = "TEXT")
    private String comment;

    /** 요리 사진 S3 퍼블릭 URL (nullable) */
    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    /** 작성 일시 — DB default CURRENT_TIMESTAMP */
    @Column(name = "REG_DT", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;
}
