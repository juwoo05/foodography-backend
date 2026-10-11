package kopo.poly.service;

import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.NoticePageDTO;

import java.util.Optional;

public interface INoticeService {

    /**
     * 공지 목록 페이지 조회 — 노출 공지만, 고정 공지 우선 후 최신순
     *
     * @param page     페이지 (0부터)
     * @param size     페이지 크기 (1~50)
     * @param category 카테고리 필터 (허용 값이 아니면 전체)
     */
    NoticePageDTO getNoticeList(int page, int size, String category) throws Exception;

    /**
     * 공지 상세 조회 — 조회수 +1, 이전·다음 글 포함
     * 없거나 숨김 공지면 Optional.empty()
     */
    Optional<NoticeDTO> getNoticeDetail(Integer noticeId) throws Exception;

    /**
     * 공지 등록 (관리자 전용 — SecurityConfig 에서 ROLE_ADMIN 검사)
     *
     * @param pDTO     category, title, content, pinnedYn
     * @param writerId 작성자 USER_ID (JWT subject)
     * @return 생성된 NOTICE_ID
     * @throws IllegalArgumentException 카테고리·제목·본문 검증 실패 시
     */
    Integer insertNotice(NoticeDTO pDTO, Integer writerId) throws Exception;

    /**
     * 공지 수정 (관리자 전용) — category, title, content, pinnedYn 변경
     *
     * @return 수정 성공 true, 없거나 숨김 공지면 false
     * @throws IllegalArgumentException 카테고리·제목·본문 검증 실패 시
     */
    boolean updateNotice(Integer noticeId, NoticeDTO pDTO) throws Exception;

    /**
     * 공지 삭제 (관리자 전용) — VISIBLE_YN = N 으로 숨김 처리
     *
     * @return 삭제 성공 true, 없거나 이미 숨김 공지면 false
     */
    boolean deleteNotice(Integer noticeId) throws Exception;
}
