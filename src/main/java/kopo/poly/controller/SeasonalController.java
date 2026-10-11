package kopo.poly.controller;

import kopo.poly.dto.SeasonalFoodDTO;
import kopo.poly.dto.SeasonalWeatherDTO;
import kopo.poly.service.ISeasonalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/seasonal")
@RequiredArgsConstructor
public class SeasonalController {

    private final ISeasonalService seasonalService;

    /**
     * 현재 날씨 — 기상청 초단기실황 (기온·습도·강수·풍속)
     * GET /api/seasonal/weather?lat=37.57&lon=126.97
     */
    @GetMapping("/weather")
    public ResponseEntity<SeasonalWeatherDTO> getWeather(
            @RequestParam double lat,
            @RequestParam double lon) {

        log.info("{}.getWeather Start | lat={} lon={}", getClass().getName(), lat, lon);

        try {
            SeasonalWeatherDTO result = seasonalService.getCurrentWeather(lat, lon);
            log.info("{}.getWeather End", getClass().getName());
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.warn("{}.getWeather 기상청 오류 | {}", getClass().getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();   // 외부 API 실패 → 프론트 에러 표시

        } catch (Exception e) {
            log.error("{}.getWeather 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 이달의 제철 식재료 — 농사로 이달의 음식 (구입요령·보관법·손질법·효능 포함)
     * GET /api/seasonal/foods?year=2026&month=10
     */
    @GetMapping("/foods")
    public ResponseEntity<List<SeasonalFoodDTO>> getFoods(
            @RequestParam int year,
            @RequestParam int month) {

        log.info("{}.getFoods Start | year={} month={}", getClass().getName(), year, month);

        if (month < 1 || month > 12) return ResponseEntity.badRequest().build();

        try {
            List<SeasonalFoodDTO> result = seasonalService.getSeasonalFoods(year, month);
            log.info("{}.getFoods End | count={}", getClass().getName(), result.size());
            return ResponseEntity.ok(result);

        } catch (RuntimeException e) {
            log.warn("{}.getFoods 농사로 오류 | {}", getClass().getName(), e.getMessage());
            return ResponseEntity.ok(List.of());   // 인증키 미승인·API 장애 → 빈 목록 (프론트 빈 상태 표시)

        } catch (Exception e) {
            log.error("{}.getFoods 오류 | {}", getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
