package kopo.poly.service;

import kopo.poly.dto.SeasonalFoodDTO;
import kopo.poly.dto.SeasonalWeatherDTO;

import java.util.List;

public interface ISeasonalService {

    /**
     * 위경도 기준 현재 날씨 조회 — 기상청 초단기실황
     *
     * <p>위경도를 기상청 격자로 변환한 뒤 가장 최근 발표 자료를 조회합니다.
     * 같은 격자·발표 시각은 Redis 에 10분 캐시합니다.</p>
     *
     * @param lat 위도
     * @param lon 경도
     */
    SeasonalWeatherDTO getCurrentWeather(double lat, double lon) throws Exception;

    /**
     * 연·월 기준 제철 식재료 목록 조회 — 농사로 이달의 음식
     *
     * <p>식재료 목록 조회 후 각 식재료 상세(구입요령·보관법·손질법·효능)를 병렬 조회합니다.
     * 요청 연도에 데이터가 없으면 전년도로 한 번 더 조회합니다.</p>
     *
     * @param year  연도
     * @param month 월 (1~12)
     */
    List<SeasonalFoodDTO> getSeasonalFoods(int year, int month) throws Exception;
}
