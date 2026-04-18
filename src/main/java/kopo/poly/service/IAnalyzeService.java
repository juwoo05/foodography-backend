package kopo.poly.service;

import kopo.poly.dto.AnalysisResultDTO;

public interface IAnalyzeService {

    /**
     * S3에 저장된 이미지를 FastAPI에 분석 요청
     *
     * @param savedFilename S3 저장 키 (예: fridge-images/uuid.jpg)
     * @return FastAPI 분석 결과
     */
    AnalysisResultDTO analyzeImage(String savedFilename);
}
