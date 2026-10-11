package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 공지사항 목록 페이지 응답
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoticePageDTO(

        /** 현재 페이지 공지 (고정 공지가 항상 앞) */
        List<NoticeDTO> notices,

        /** 현재 페이지 (0부터) */
        Integer page,

        /** 페이지 크기 */
        Integer size,

        /** 전체 페이지 수 */
        Integer totalPages,

        /** 전체 공지 수 */
        Long totalCount

) {}
