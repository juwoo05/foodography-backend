package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * NOTICE 테이블 DTO
 *
 * 목록 응답 : content, prev/next 제외
 * 상세 응답 : content + 이전·다음 글 포함
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticeDTO(

        /** PK */
        Integer noticeId,

        /** GENERAL / UPDATE / EVENT / MAINTENANCE */
        String category,

        /** 제목 */
        String title,

        /** 본문 — 일반 텍스트 (상세 조회만) */
        String content,

        /** 상단 고정 여부 Y/N */
        String pinnedYn,

        /** 조회수 */
        Integer viewCnt,

        /** 작성 일시 (yyyy-MM-dd HH:mm) */
        String regDt,

        /** 이전 글 (더 오래된 글) — 없으면 null */
        Integer prevId,
        String  prevTitle,

        /** 다음 글 (더 최근 글) — 없으면 null */
        Integer nextId,
        String  nextTitle

) {}
