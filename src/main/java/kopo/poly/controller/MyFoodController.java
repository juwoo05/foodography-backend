package kopo.poly.controller;

import kopo.poly.dto.FoodDbDTO;
import kopo.poly.dto.MsgDTO;
import kopo.poly.persistance.mongodb.IMyFoodMapper;
import kopo.poly.service.IMyFoodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/food")
@RequiredArgsConstructor
public class MyFoodController {

    private final IMyFoodService myFoodService;
    private final IMyFoodMapper  myFoodMapper;

    /**
     * 식재료 영양 정보 매칭 요청
     *
     * <p>React → POST /api/food/match (body 없음)<br>
     * MongoDB FOOD_AFTER 최신 문서의 scanId · ingredients 를 자동 로딩합니다.<br>
     * 동일 scanId 재요청 시 Redis 캐시에서 즉시 반환 (Pinecone · Claude 미호출)</p>
     */
    @PostMapping("/match")
    public ResponseEntity<List<FoodDbDTO>> matchIngredients(
            @AuthenticationPrincipal Jwt jwt) {

        Integer userId = Integer.parseInt(jwt.getSubject());
        log.info("{}.matchIngredients Start! userId={}", this.getClass().getName(), userId);

        try {
            // FOOD_AFTER 최신 문서 → scanId + ingredients 확보 (userId 소유권 검증 포함)
            FoodDbDTO latest = myFoodMapper.getLatestIngredients("FOOD_AFTER", userId);

            log.info("{}.matchIngredients FOOD_AFTER 조회 완료 scanId={} ingredientCount={}",
                    this.getClass().getName(), latest.scanId(),
                    latest.ingredients() != null ? latest.ingredients().size() : 0);

            // FOOD_AFTER 문서 없음 — 사진 업로드 전 상태이므로 빈 목록 반환
            if (latest.scanId() == null || latest.ingredients() == null || latest.ingredients().isEmpty()) {
                log.warn("{}.matchIngredients scanId 또는 식재료 없음 — 사진 분석 후 호출 필요 userId={}",
                        this.getClass().getName(), userId);
                return ResponseEntity.ok(List.of());
            }

            List<FoodDbDTO> rList = myFoodService.matchIngredients(latest.scanId(), latest.ingredients());

            log.info("{}.matchIngredients End! resultCount={}", this.getClass().getName(), rList.size());

            return ResponseEntity.ok(rList);

        } catch (Exception e) {
            log.error("{}.matchIngredients 오류 | {}", this.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    /**
     * 식품 DB 수동 인덱싱 (개발·운영 검증용)
     *
     * <p>스케줄러 자동 실행 외에 즉시 인덱싱이 필요할 때 사용합니다.</p>
     * POST /api/food/index
     */
    @PostMapping("/index")
    public ResponseEntity<MsgDTO> indexFoodDatabase() {

        log.info("{}.indexFoodDatabase Start!", this.getClass().getName());

        try {
            myFoodService.indexFoodDatabase();
            log.info("{}.indexFoodDatabase End!", this.getClass().getName());
            return ResponseEntity.ok(new MsgDTO(1, "식품 DB 인덱싱 완료"));

        } catch (Exception e) {
            log.error("{}.indexFoodDatabase 오류 | {}", this.getClass().getName(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new MsgDTO(0, "인덱싱 실패: " + e.getMessage()));
        }
    }
}
