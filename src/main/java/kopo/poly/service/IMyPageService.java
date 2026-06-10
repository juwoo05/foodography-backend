package kopo.poly.service;

import kopo.poly.dto.MyPageDTO;

public interface IMyPageService {

    /**
     * 마이페이지 정보 조회
     * — 유저 기본 정보 + 활동 통계 + 최근 레시피·리뷰
     *
     * @param userId JWT 에서 추출한 유저 ID
     * @return 마이페이지 종합 DTO
     */
    MyPageDTO getMyPageInfo(Integer userId) throws Exception;
}
