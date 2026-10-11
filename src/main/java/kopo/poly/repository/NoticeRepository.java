package kopo.poly.repository;

import kopo.poly.repository.entity.NoticeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<NoticeEntity, Integer> {

    /** 노출 공지 목록 — 정렬은 Pageable 로 전달 (PINNED_YN DESC, REG_DT DESC) */
    Page<NoticeEntity> findByVisibleYn(String visibleYn, Pageable pageable);

    /** 노출 공지 목록 — 카테고리 필터 */
    Page<NoticeEntity> findByVisibleYnAndCategory(String visibleYn, String category, Pageable pageable);

    /** 노출 공지 단건 — 숨김 공지는 조회되지 않음 */
    Optional<NoticeEntity> findByNoticeIdAndVisibleYn(Integer noticeId, String visibleYn);

    /** 이전 글 (더 오래된 글) */
    Optional<NoticeEntity> findFirstByVisibleYnAndNoticeIdLessThanOrderByNoticeIdDesc(String visibleYn, Integer noticeId);

    /** 다음 글 (더 최근 글) */
    Optional<NoticeEntity> findFirstByVisibleYnAndNoticeIdGreaterThanOrderByNoticeIdAsc(String visibleYn, Integer noticeId);

    /** 조회수 +1 — 동시 조회 시에도 누락 없도록 DB 에서 증가 */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE NoticeEntity n SET n.viewCnt = n.viewCnt + 1 WHERE n.noticeId = :noticeId")
    int increaseViewCnt(@Param("noticeId") Integer noticeId);
}
