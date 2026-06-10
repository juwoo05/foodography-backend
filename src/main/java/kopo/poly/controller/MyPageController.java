package kopo.poly.controller;

import kopo.poly.dto.MyPageDTO;
import kopo.poly.service.IMyPageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final IMyPageService myPageService;

    /**
     * 마이페이지 정보 조회
     * GET /api/mypage/profile
     * — 유저 기본 정보 + 활동 통계 + 최근 레시피·리뷰
     */
    @GetMapping("/profile")
    public ResponseEntity<MyPageDTO> getProfile(
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.getProfile Start | userId={}", getClass().getName(), userId);

        try {
            MyPageDTO result = myPageService.getMyPageInfo(userId);
            log.info("{}.getProfile End | userId={}", getClass().getName(), userId);
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.warn("{}.getProfile 비즈니스 오류 | {}", getClass().getName(), e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("{}.getProfile 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
