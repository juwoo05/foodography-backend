package kopo.poly.service;

public interface ITokenRedisService {

    /**
     * Refresh Token 저장 — RT:{userId} = token, TTL = refreshTtlSeconds
     * 로그인·재발급 시 호출
     */
    void saveRefreshToken(Integer userId, String token, long ttlSeconds);

    /** Refresh Token 조회 */
    String getRefreshToken(Integer userId);

    /** Refresh Token 삭제 — 로그아웃·탈취 감지 시 호출 */
    void deleteRefreshToken(Integer userId);

    /**
     * Access Token 블랙리스트 등록 — BL:AT:{jti} = "1", TTL = remainingSeconds
     * 로그아웃 시 호출
     */
    void blacklistAccessToken(String jti, long remainingSeconds);

    /** Access Token 블랙리스트 여부 확인 */
    boolean isBlacklisted(String jti);
}
