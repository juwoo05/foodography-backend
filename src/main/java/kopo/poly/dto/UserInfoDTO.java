package kopo.poly.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record UserInfoDTO(

        Integer userId, // 회원아이디

        String userName, // 회원 이름

        String phoneNum, // 전화번호

        String email, // 이메일

        String password, // 비밀번호

        LocalDateTime regDt, // 등록일시

        String existYn, // 이메일 존재여부

        String roles, // 회원 권한

        int authNumber // 이메일 중복체크를 위한 인증번호
) {
}
