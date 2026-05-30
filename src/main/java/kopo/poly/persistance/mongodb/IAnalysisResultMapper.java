package kopo.poly.persistance.mongodb;

import kopo.poly.dto.AnalysisResultDTO;

import java.util.List;

public interface IAnalysisResultMapper {

    int insertDate(AnalysisResultDTO pDTO, String colNm) throws Exception;

    /** userId 로 소유권 검증 후 해당 scanId 의 식재료명 목록 조회 */
    List<String> getFoodResult(String colNm, String scanId, Integer userId) throws Exception;
}
