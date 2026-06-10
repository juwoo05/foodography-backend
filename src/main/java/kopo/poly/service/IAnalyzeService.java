package kopo.poly.service;

import kopo.poly.dto.AnalysisResultDTO;
import kopo.poly.dto.RecipeDTO;
import kopo.poly.dto.VideoSummaryDTO;

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

    /** FOOD_AFTER 컬렉션에서 userId + scanId 소유권 검증 후 식재료명 기반 레시피 분석 */
    List<RecipeDTO> analyzeRecipes(String scanId, Integer userId) throws Exception;

    /**
     * 사용자가 레시피 선택 후 호출 — YouTube URL → Gemini 영상 분석 → 구조화된 조리 단계 목록
     * RECIPE 테이블에 USER_ID 와 영상 요약 단계를 함께 UPDATE
     *
     * @param youtubeUrl 분석할 YouTube 영상 URL
     * @param scanId     현재 분석 세션 ID
     * @param userId     JWT Subject 에서 추출한 사용자 ID
     * @return 조리 단계 목록 (VideoSummaryDTO 리스트)
     */
    List<VideoSummaryDTO> getVideoSummary(String youtubeUrl, String scanId, Integer userId) throws Exception;
}
