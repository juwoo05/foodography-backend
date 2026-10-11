package kopo.poly.exception;

import kopo.poly.dto.MsgDTO;
import kopo.poly.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.security.core.AuthenticationException;

@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<CommonResponse<MsgDTO>> handleAuthentication(AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getClass().getSimpleName());

        String msg;
        if (ex instanceof BadCredentialsException) {
            msg = "아이디 또는 비밀번호가 올바르지 않습니다.";
        } else if (ex instanceof LockedException) {
            msg = "계정이 잠겨 있습니다.";
        } else if (ex instanceof DisabledException) {
            msg = "비활성화된 계정입니다.";
        } else {
            msg = "로그인에 실패했습니다.";
        }

        MsgDTO dto = MsgDTO.builder()
                .result(0)
                .msg(msg)
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.of(
                        HttpStatus.UNAUTHORIZED,
                        HttpStatus.UNAUTHORIZED.series().name(),
                        dto));
    }
}
