package kopo.poly.controller;

import kopo.poly.dto.FridgeScanDTO;
import kopo.poly.dto.SpecialDayDTO;
import kopo.poly.service.IFridgeCalendarService;
import kopo.poly.service.ISpecialDayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 냉장고 달력 — 스캔 확정 식료품 조회 (조회 전용)
 */
@Slf4j
@RestController
@RequestMapping("/api/fridge")
@RequiredArgsConstructor
public class FridgeCalendarController {

    private final IFridgeCalendarService fridgeCalendarService;
    private final ISpecialDayService     specialDayService;

    /**
     * 기간 내 스캔 목록
     *
     * GET /api/fridge/calendar?start=2026-09-28&end=2026-11-09
     * start 포함 · end 제외 (KST) — FullCalendar 표시 범위 그대로 전달
     */
    @GetMapping("/calendar")
    public ResponseEntity<List<FridgeScanDTO>> getCalendar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String start,
            @RequestParam String end) {

        log.info("{}.getCalendar Start!", this.getClass().getName());

        Integer userId = Integer.parseInt(jwt.getSubject());

        try {
            List<FridgeScanDTO> rList = fridgeCalendarService.getScans(userId, start, end);

            log.info("{}.getCalendar End! userId={} count={}", this.getClass().getName(), userId, rList.size());

            return ResponseEntity.ok(rList);

        } catch (IllegalArgumentException e) {
            log.warn("냉장고 달력 요청 오류 | userId={} {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(List.of());

        } catch (Exception e) {
            log.error("냉장고 달력 조회 실패 | userId={} {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * 연도별 특일 (공휴일·잡절·24절기·기념일) — 한국천문연구원 특일 정보
     *
     * GET /api/fridge/special-days?year=2026
     * 외부 API 실패 시에도 200 + 빈 목록 (달력 본 기능에 영향 없도록)
     */
    @GetMapping("/special-days")
    public ResponseEntity<List<SpecialDayDTO>> getSpecialDays(@RequestParam int year) {

        log.info("{}.getSpecialDays Start! year={}", this.getClass().getName(), year);

        if (year < 2000 || year > 2100) {
            log.warn("특일 요청 연도 오류 | year={}", year);
            return ResponseEntity.badRequest().body(List.of());
        }

        List<SpecialDayDTO> rList = specialDayService.getSpecialDays(year);

        log.info("{}.getSpecialDays End! year={} count={}", this.getClass().getName(), year, rList.size());

        return ResponseEntity.ok(rList);
    }
}
