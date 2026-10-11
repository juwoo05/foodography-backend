package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 특일 1건 (한국천문연구원 특일 정보)
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpecialDayDTO(

        /** 날짜 (yyyy-MM-dd) */
        String date,

        /** 명칭 (예: 추석, 초복, 입동, 김치의 날) */
        String name,

        /** 분류 — HOLIDAY(공휴일) · SUNDRY(잡절) · DIVISION(24절기) · ANNIVERSARY(기념일) */
        String kind,

        /** 공공기관 휴일 여부 */
        Boolean holiday
) {
}
