package kopo.poly.service;

import kopo.poly.dto.UserInfoDTO;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface IUserInfoService extends UserDetailsService {

    UserInfoDTO getUserEmailExists(UserInfoDTO pDTO) throws Exception;

    UserInfoDTO searchUserEmail(UserInfoDTO pDTO) throws Exception;

    int updatePassword(UserInfoDTO pDTO) throws Exception;

    /**
     * 이메일 인증코드 발송 + Redis 저장 (TTL 5분)
     * HttpSession 을 받지 않음 — 서비스 계층은 HTTP 기술에 무관
     */
    int sendEmailAuthCode(UserInfoDTO pDTO) throws Exception;

    /**
     * Redis 에 저장된 인증코드와 사용자 입력값 비교
     * 일치 시 Redis 키 즉시 삭제 (1회성 보장)
     */
    int verifyEmailCode(String email, String inputCode);

    int insertUserInfo(UserInfoDTO pDTO) throws Exception;

    int getUserLogin(UserInfoDTO pDTO) throws Exception;
}
