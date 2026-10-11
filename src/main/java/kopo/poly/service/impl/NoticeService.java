package kopo.poly.service.impl;

import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.NoticePageDTO;
import kopo.poly.repository.NoticeRepository;
import kopo.poly.repository.entity.NoticeEntity;
import kopo.poly.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService implements INoticeService {

    private final NoticeRepository noticeRepository;

    private static final String VISIBLE = "Y";

    /** 허용 카테고리 — 그 외 값은 전체 조회 */
    private static final Set<String> CATEGORIES = Set.of("GENERAL", "UPDATE", "EVENT", "MAINTENANCE");

    private static final int MAX_SIZE = 50;

    /** 제목 최대 길이 — NOTICE.TITLE VARCHAR(200) */
    private static final int TITLE_MAX = 200;

    /** 고정 공지 우선 → 최신순 (같은 시각이면 ID 역순) */
    private static final Sort LIST_SORT = Sort.by(
            Sort.Order.desc("pinnedYn"),
            Sort.Order.desc("regDt"),
            Sort.Order.desc("noticeId"));

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── 목록 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public NoticePageDTO getNoticeList(int page, int size, String category) throws Exception {

        int    safePage = Math.max(page, 0);
        int    safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        String cat      = category == null ? null : category.trim().toUpperCase();
        if (cat != null && !CATEGORIES.contains(cat)) cat = null;

        log.info("{}.getNoticeList Start | page={} size={} category={}",
                this.getClass().getName(), safePage, safeSize, cat);

        PageRequest pageable = PageRequest.of(safePage, safeSize, LIST_SORT);
        Page<NoticeEntity> result = cat == null
                ? noticeRepository.findByVisibleYn(VISIBLE, pageable)
                : noticeRepository.findByVisibleYnAndCategory(VISIBLE, cat, pageable);

        // 목록에는 본문 제외
        List<NoticeDTO> notices = result.getContent().stream()
                .map(e -> NoticeDTO.builder()
                        .noticeId(e.getNoticeId())
                        .category(e.getCategory())
                        .title   (e.getTitle())
                        .pinnedYn(e.getPinnedYn())
                        .viewCnt (e.getViewCnt())
                        .regDt   (formatDt(e))
                        .build())
                .toList();

        log.info("{}.getNoticeList End | count={} total={}",
                this.getClass().getName(), notices.size(), result.getTotalElements());

        return NoticePageDTO.builder()
                .notices   (notices)
                .page      (safePage)
                .size      (safeSize)
                .totalPages(result.getTotalPages())
                .totalCount(result.getTotalElements())
                .build();
    }

    // ── 상세 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Optional<NoticeDTO> getNoticeDetail(Integer noticeId) throws Exception {

        log.info("{}.getNoticeDetail Start | noticeId={}", this.getClass().getName(), noticeId);

        Optional<NoticeEntity> found = noticeRepository.findByNoticeIdAndVisibleYn(noticeId, VISIBLE);
        if (found.isEmpty()) {
            log.info("{}.getNoticeDetail End | 없음 noticeId={}", this.getClass().getName(), noticeId);
            return Optional.empty();
        }

        NoticeEntity e = found.get();
        noticeRepository.increaseViewCnt(noticeId);

        Optional<NoticeEntity> prev = noticeRepository
                .findFirstByVisibleYnAndNoticeIdLessThanOrderByNoticeIdDesc(VISIBLE, noticeId);
        Optional<NoticeEntity> next = noticeRepository
                .findFirstByVisibleYnAndNoticeIdGreaterThanOrderByNoticeIdAsc(VISIBLE, noticeId);

        NoticeDTO dto = NoticeDTO.builder()
                .noticeId (e.getNoticeId())
                .category (e.getCategory())
                .title    (e.getTitle())
                .content  (e.getContent())
                .pinnedYn (e.getPinnedYn())
                .viewCnt  (e.getViewCnt() + 1)   // 방금 증가분 반영
                .regDt    (formatDt(e))
                .prevId   (prev.map(NoticeEntity::getNoticeId).orElse(null))
                .prevTitle(prev.map(NoticeEntity::getTitle).orElse(null))
                .nextId   (next.map(NoticeEntity::getNoticeId).orElse(null))
                .nextTitle(next.map(NoticeEntity::getTitle).orElse(null))
                .build();

        log.info("{}.getNoticeDetail End | noticeId={}", this.getClass().getName(), noticeId);
        return Optional.of(dto);
    }

    // ── 등록 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Integer insertNotice(NoticeDTO pDTO, Integer writerId) throws Exception {

        log.info("{}.insertNotice Start | writerId={}", this.getClass().getName(), writerId);

        NoticeDTO v = validate(pDTO);

        NoticeEntity saved = noticeRepository.save(NoticeEntity.builder()
                .category (v.category())
                .title    (v.title())
                .content  (v.content())
                .pinnedYn (v.pinnedYn())
                .visibleYn(VISIBLE)
                .viewCnt  (0)
                .writerId (writerId)
                .build());

        log.info("{}.insertNotice End | noticeId={}", this.getClass().getName(), saved.getNoticeId());
        return saved.getNoticeId();
    }

    // ── 수정 ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean updateNotice(Integer noticeId, NoticeDTO pDTO) throws Exception {

        log.info("{}.updateNotice Start | noticeId={}", this.getClass().getName(), noticeId);

        NoticeDTO v = validate(pDTO);

        Optional<NoticeEntity> found = noticeRepository.findByNoticeIdAndVisibleYn(noticeId, VISIBLE);
        if (found.isEmpty()) {
            log.info("{}.updateNotice End | 없음 noticeId={}", this.getClass().getName(), noticeId);
            return false;
        }

        // 변경 감지(dirty checking) 로 UPDATE — CHG_DT 는 DB ON UPDATE 로 갱신
        found.get().update(v.category(), v.title(), v.content(), v.pinnedYn());

        log.info("{}.updateNotice End | noticeId={}", this.getClass().getName(), noticeId);
        return true;
    }

    // ── 삭제 (숨김) ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public boolean deleteNotice(Integer noticeId) throws Exception {

        log.info("{}.deleteNotice Start | noticeId={}", this.getClass().getName(), noticeId);

        Optional<NoticeEntity> found = noticeRepository.findByNoticeIdAndVisibleYn(noticeId, VISIBLE);
        if (found.isEmpty()) {
            log.info("{}.deleteNotice End | 없음 noticeId={}", this.getClass().getName(), noticeId);
            return false;
        }

        found.get().hide();

        log.info("{}.deleteNotice End | noticeId={}", this.getClass().getName(), noticeId);
        return true;
    }

    /**
     * 등록·수정 입력 검증 + 정규화
     * 카테고리 대문자, 제목·본문 공백 제거, pinnedYn Y/N
     *
     * @throws IllegalArgumentException 검증 실패 시 (메시지는 화면에 그대로 표시)
     */
    private NoticeDTO validate(NoticeDTO pDTO) {

        String category = pDTO.category() == null ? "" : pDTO.category().trim().toUpperCase();
        String title    = pDTO.title()    == null ? "" : pDTO.title().trim();
        String content  = pDTO.content()  == null ? "" : pDTO.content().strip();

        if (!CATEGORIES.contains(category)) throw new IllegalArgumentException("분류를 선택해주세요.");
        if (title.isEmpty())                throw new IllegalArgumentException("제목을 입력해주세요.");
        if (title.length() > TITLE_MAX)     throw new IllegalArgumentException("제목은 " + TITLE_MAX + "자 이하로 입력해주세요.");
        if (content.isEmpty())              throw new IllegalArgumentException("내용을 입력해주세요.");

        return NoticeDTO.builder()
                .category(category)
                .title   (title)
                .content (content)
                .pinnedYn("Y".equalsIgnoreCase(pDTO.pinnedYn()) ? "Y" : "N")
                .build();
    }

    private String formatDt(NoticeEntity e) {
        return e.getRegDt() != null ? e.getRegDt().format(DT_FMT) : "";
    }
}
