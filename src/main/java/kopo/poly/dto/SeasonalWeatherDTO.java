package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 제철 식재료 페이지 — 현재 날씨 DTO
 *
 * <p>기상청 초단기실황(getUltraSrtNcst) 응답의 category 별 obsrValue 를 담습니다.</p>
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SeasonalWeatherDTO(

        /** 기온 (℃) ← T1H */
        Double temperature,

        /** 습도 (%) ← REH */
        Integer humidity,

        /** 1시간 강수량 (mm) ← RN1 */
        Double rn1,

        /** 강수형태 ← PTY (0 없음, 1 비, 2 비/눈, 3 눈, 5 빗방울, 6 빗방울눈날림, 7 눈날림) */
        Integer pty,

        /** 풍속 (m/s) ← WSD */
        Double windSpeed,

        /** 지역명 — 기상청은 주소를 주지 않으므로 현재는 null (프론트에서 대체 문구 표시) */
        String locationName,

        /** 발표 일시 (yyyy-MM-ddTHH:mm) ← base_date + base_time */
        String baseDateTime,

        /** 기상청 격자 X 좌표 */
        Integer nx,

        /** 기상청 격자 Y 좌표 */
        Integer ny

) {}
