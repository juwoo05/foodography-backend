package kopo.poly.service;

import kopo.poly.dto.FridgeScanDTO;

import java.util.List;

public interface IFridgeCalendarService {

    /**
     * 기간 내 스캔 확정 식료품 조회 (냉장고 달력)
     *
     * @param userId 사용자 ID
     * @param start  시작일 yyyy-MM-dd (포함, KST)
     * @param end    종료일 yyyy-MM-dd (제외, KST)
     * @return 스캔 목록 (스캔 시각 오름차순)
     * @throws IllegalArgumentException 날짜 형식 오류·기간 초과 시
     * @throws Exception                MongoDB 조회 오류 시
     */
    List<FridgeScanDTO> getScans(Integer userId, String start, String end) throws Exception;
}
