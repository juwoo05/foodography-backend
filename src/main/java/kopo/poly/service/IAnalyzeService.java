package kopo.poly.service;

import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.dto.RecipeDTO;

import java.util.List;

public interface IAnalyzeService {

    /**
     * S3에 저장된 이미지를 FastAPI에 분석 요청
     *
     * @param savedFilename S3 저장 키 (예: fridge-images/uuid.jpg)
     * @return FastAPI 분석 결과
     */
    AnalysisResultDTO analyzeImage(String savedFilename);

    /** AI 원본 결과 → FOOD_BEFORE 저장 (scanId 포함) */
    int saveFoodResult(AnalysisResultDTO pDTO) throws Exception;

    /** 사용자 수정 결과 → FOOD_AFTER 저장 */
    int saveFoodAfterResult(AnalysisResultDTO pDTO) throws Exception;

    /** FOOD_AFTER 컬렉션에서 특정 scanId의 식재료명 목록 조회 */
    List<RecipeDTO> analyzeRecipes(String scanId) throws Exception;
}
