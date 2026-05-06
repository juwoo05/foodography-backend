package kopo.poly.persistance.mongodb;

import kopo.poly.dto.AnalysisResultDTO;

import java.util.List;

public interface IAnalysisResultMapper {

    int insertDate(AnalysisResultDTO pDTO, String colNm) throws Exception;

    List<String> getFoodResult(String colNm, String scanId) throws Exception;
}
