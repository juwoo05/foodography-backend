package kopo.poly.service;

import kopo.poly.dto.SpecialDayDTO;

import java.util.List;

public interface ISpecialDayService {

    /**
     * 연도별 특일 (공휴일·잡절·24절기·기념일) — 날짜 오름차순
     *
     * <p>외부 API 일부가 실패하면 해당 분류만 빠지고, 전부 실패하면 빈 목록을 반환합니다.</p>
     *
     * @param year 연도
     * @return 특일 목록
     */
    List<SpecialDayDTO> getSpecialDays(int year);
}
