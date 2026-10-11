package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 조리 과정 단일 단계 DTO.
 * FastAPI VideoSummaryStep 과 1:1 대응.
 *
 * startSeconds / endSeconds 는 프론트엔드가 YouTube Player API 로
 * 해당 구간을 직접 탐색할 때 사용.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VideoSummaryDTO(
        String  stepName,       // 예: "양념 준비"
        String  description,    // 예: "간장·설탕·다진 마늘을 섞는다"
        Integer startSeconds,   // 예: 37  (프론트엔드 영상 조작용)
        Integer endSeconds,     // 예: 89
        String  displayTime     // 예: "0:37-1:29" (화면 표출용)
) {
}
