package kopo.poly.controller;

import kopo.poly.dto.NoticeDTO;
import kopo.poly.dto.NoticePageDTO;
import kopo.poly.response.CommonResponse;
import kopo.poly.service.INoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * 공지사항 API
 * 조회 — 비로그인 허용 (SecurityConfig GET /api/notice/** permitAll)
 * 등록·수정·삭제 — 관리자 전용 (SecurityConfig POST·PUT·DELETE /api/notice/** hasRole ADMIN)
 */
@Slf4j
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final INoticeService noticeService;

    /**
     * GET /api/notice/list?page=0&size=10&category=UPDATE
     * 공지 목록 (고정 공지 우선 → 최신순)
     */
    @GetMapping("/list")
    public ResponseEntity<NoticePageDTO> getNoticeList(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    String category) throws Exception {

        log.info("{}.getNoticeList Start | page={} size={} category={}",
                this.getClass().getName(), page, size, category);

        NoticePageDTO result = noticeService.getNoticeList(page, size, category);

        log.info("{}.getNoticeList End | count={}", this.getClass().getName(), result.notices().size());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/notice/{noticeId}
     * 공지 상세 (조회수 +1, 이전·다음 글 포함) — 없거나 숨김이면 404
     */
    @GetMapping("/{noticeId}")
    public ResponseEntity<NoticeDTO> getNoticeDetail(@PathVariable Integer noticeId) throws Exception {

        log.info("{}.getNoticeDetail Start | noticeId={}", this.getClass().getName(), noticeId);

        return noticeService.getNoticeDetail(noticeId)
                .map(dto -> {
                    log.info("{}.getNoticeDetail End | noticeId={}", this.getClass().getName(), noticeId);
                    return ResponseEntity.ok(dto);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/notice
     * 공지 등록 — 관리자 전용 (SecurityConfig POST /api/notice hasRole ADMIN)
     * 요청 바디: { category, title, content, pinnedYn }
     * 응답 data: 생성된 noticeId
     * writerId 는 JWT Subject 에서 추출 — 클라이언트 전달값 무시
     */
    @PostMapping
    public ResponseEntity<CommonResponse<Integer>> insertNotice(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody NoticeDTO dto) throws Exception {

        Integer writerId = Integer.parseInt(jwt.getSubject());
        log.info("{}.insertNotice Start | writerId={}", this.getClass().getName(), writerId);

        try {
            Integer noticeId = noticeService.insertNotice(dto, writerId);

            log.info("{}.insertNotice End | noticeId={}", this.getClass().getName(), noticeId);
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "공지사항이 등록되었습니다.", noticeId));

        } catch (IllegalArgumentException e) {
            log.warn("공지 등록 요청 오류 | writerId={} {}", writerId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResponse.of(HttpStatus.BAD_REQUEST, e.getMessage(), null));
        }
    }

    /**
     * PUT /api/notice/{noticeId}
     * 공지 수정 — 관리자 전용 (SecurityConfig PUT /api/notice/** hasRole ADMIN)
     * 요청 바디: { category, title, content, pinnedYn }
     * 없거나 숨김 공지면 404
     */
    @PutMapping("/{noticeId}")
    public ResponseEntity<CommonResponse<Integer>> updateNotice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer noticeId,
            @RequestBody NoticeDTO dto) throws Exception {

        log.info("{}.updateNotice Start | noticeId={} adminId={}", this.getClass().getName(), noticeId, jwt.getSubject());

        try {
            if (!noticeService.updateNotice(noticeId, dto)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResponse.of(HttpStatus.NOT_FOUND, "없는 공지이거나 삭제된 공지입니다.", null));
            }

            log.info("{}.updateNotice End | noticeId={}", this.getClass().getName(), noticeId);
            return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "공지사항이 수정되었습니다.", noticeId));

        } catch (IllegalArgumentException e) {
            log.warn("공지 수정 요청 오류 | noticeId={} {}", noticeId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResponse.of(HttpStatus.BAD_REQUEST, e.getMessage(), null));
        }
    }

    /**
     * DELETE /api/notice/{noticeId}
     * 공지 삭제 — 관리자 전용 (SecurityConfig DELETE /api/notice/** hasRole ADMIN)
     * 행을 지우지 않고 VISIBLE_YN = N 으로 숨김 (SQL 로 복구 가능)
     * 없거나 이미 숨김 공지면 404
     */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<CommonResponse<Integer>> deleteNotice(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer noticeId) throws Exception {

        log.info("{}.deleteNotice Start | noticeId={} adminId={}", this.getClass().getName(), noticeId, jwt.getSubject());

        if (!noticeService.deleteNotice(noticeId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.of(HttpStatus.NOT_FOUND, "없는 공지이거나 이미 삭제된 공지입니다.", null));
        }

        log.info("{}.deleteNotice End | noticeId={}", this.getClass().getName(), noticeId);
        return ResponseEntity.ok(CommonResponse.of(HttpStatus.OK, "공지사항이 삭제되었습니다.", noticeId));
    }
}
