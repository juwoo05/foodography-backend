package kopo.poly.dto;

import lombok.Builder;

@Builder
public record MsgDTO(

        int result,

        String msg
) {
}
