package kopo.poly.repository.entity;

import jakarta.persistence.*;
import kopo.poly.dto.VideoSummaryDTO;
import kopo.poly.repository.entity.converter.StringListConverter;
import kopo.poly.repository.entity.converter.VideoSummaryListConverter;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@Table(
        name = "RECIPE",
        indexes = {
                @Index(name = "IDX_RECIPE_SCAN_ID",      columnList = "SCAN_ID"),
                @Index(name = "IDX_RECIPE_SCAN_YOUTUBE", columnList = "SCAN_ID, YOUTUBE_URL(255)")
        }
)
public class RecipeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RECIPE_ID", updatable = false)
    private Integer recipeId;

    /** FK → USER_INFO.USER_ID  (getVideoSummary 호출 시 채워짐 — 초기 INSERT 시 NULL 허용) */
    @Column(name = "USER_ID")
    private Integer userId;

    @NonNull
    @Column(name = "SCAN_ID", nullable = false, length = 36)
    private String scanId;

    @NonNull
    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Column(name = "CONTENT", columnDefinition = "TEXT")
    private String content;

    @Column(name = "DIFFICULTY", length = 20)
    private String difficulty;

    @Column(name = "COOKING_TIME")
    private Integer cookingTime;

    @Column(name = "CALORIES")
    private Integer calories;

    @Column(name = "YOUTUBE_URL", length = 500)
    private String youtubeUrl;

    @Column(name = "YOUTUBE_URL_THUMBNAIL", length = 500)
    private String youtubeUrlThumbnail;

    /** 보유 식재료 목록 — ["계란", "두부", ...] */
    @NonNull
    @Convert(converter = StringListConverter.class)
    @Column(name = "INGREDIENTS", nullable = false, columnDefinition = "JSON")
    private List<String> ingredients;

    /** 추가 구매 필요 식재료 목록 */
    @Convert(converter = StringListConverter.class)
    @Column(name = "ADDITIONAL_INGREDIENTS", nullable = false, columnDefinition = "JSON")
    @Builder.Default
    private List<String> additionalIngredients = List.of();

    /**
     * 영상 요약 조리 단계 — NULL 허용
     * analyzeRecipes 저장 시 NULL, getVideoSummary 호출 후 업데이트
     */
    @Convert(converter = VideoSummaryListConverter.class)
    @Column(name = "RECIPE_VIDEO_SUMMARY", columnDefinition = "JSON")
    private List<VideoSummaryDTO> recipeVideoSummary;

    @Column(name = "REG_DT", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;

    /** 영상 요약 단계 + 선택한 사용자 ID 업데이트 — setter 대신 목적 메서드 사용 */
    public void updateVideoSummary(List<VideoSummaryDTO> steps, Integer userId) {
        this.recipeVideoSummary = steps;
        this.userId             = userId;
    }
}
