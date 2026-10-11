package kopo.poly.repository.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * NOTICE 테이블 JPA 엔티티 (DDL: server/sql/NOTICE.sql)
 * 작성·수정·삭제(숨김)는 관리자 전용 API (POST·PUT·DELETE /api/notice)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicInsert
@DynamicUpdate
@Entity
@Table(
        name = "NOTICE",
        indexes = {
                @Index(name = "IDX_NOTICE_LIST", columnList = "VISIBLE_YN, PINNED_YN, REG_DT")
        }
)
public class NoticeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTICE_ID", updatable = false)
    private Integer noticeId;

    /** GENERAL 공지 / UPDATE 업데이트 / EVENT 이벤트 / MAINTENANCE 점검 */
    @Column(name = "CATEGORY", nullable = false, length = 20)
    private String category;

    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    /** 본문 — 일반 텍스트 (줄바꿈 유지, HTML 미허용) */
    @Column(name = "CONTENT", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 상단 고정 여부 Y/N */
    @Column(name = "PINNED_YN", nullable = false, length = 1)
    private String pinnedYn;

    /** 노출 여부 Y/N — 삭제 대신 N */
    @Column(name = "VISIBLE_YN", nullable = false, length = 1)
    private String visibleYn;

    @Column(name = "VIEW_CNT", nullable = false)
    private Integer viewCnt;

    /** FK → USER_INFO.USER_ID (nullable) — 등록한 관리자 */
    @Column(name = "WRITER_ID")
    private Integer writerId;

    /** 작성 일시 — DB default CURRENT_TIMESTAMP */
    @Column(name = "REG_DT", updatable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime regDt;

    /** 수정 일시 — DB ON UPDATE CURRENT_TIMESTAMP */
    @Column(name = "CHG_DT", insertable = false, updatable = false)
    private LocalDateTime chgDt;

    /** 공지 수정 — 검증은 NoticeService 에서 완료된 값만 전달 */
    public void update(String category, String title, String content, String pinnedYn) {
        this.category = category;
        this.title    = title;
        this.content  = content;
        this.pinnedYn = pinnedYn;
    }

    /** 공지 삭제 — 행을 지우지 않고 숨김 처리 (VISIBLE_YN = N) */
    public void hide() {
        this.visibleYn = "N";
    }
}
