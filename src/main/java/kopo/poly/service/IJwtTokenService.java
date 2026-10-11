package kopo.poly.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.poly.dto.UserInfoDTO;

public interface IJwtTokenService {

    String generateAccessToken(UserInfoDTO user);
    String generateRefreshToken(UserInfoDTO user);
    void writeTokensAsCookies(HttpServletResponse res, String accessToken, String refreshToken);

    /**
     * 로그인 시: AT(jti 포함) + RT 발급, RT → Redis RT:{userId} 저장, 쿠키 세팅
     */
    void issueTokens(UserInfoDTO user, HttpServletResponse res);

    /**
     * 로그아웃 시: AT jti → Redis 블랙리스트 등록, RT → Redis 삭제, 쿠키 만료
     * Redis 작업 실패해도 쿠키는 항상 삭제 (best-effort)
     */
    void revokeTokens(HttpServletRequest req, HttpServletResponse res);

    /**
     * 토큰 재발급: RT 쿠키 검증 → Redis 값과 비교 → 새 AT+RT 발급
     * RT 불일치(탈취 의심) 시 RT 삭제 + RuntimeException
     */
    void refreshTokens(HttpServletRequest req, HttpServletResponse res);
}
