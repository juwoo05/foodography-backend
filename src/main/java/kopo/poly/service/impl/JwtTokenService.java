package kopo.poly.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IJwtTokenService;
import kopo.poly.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService implements IJwtTokenService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLES    = "roles";
    private static final String CLAIM_TYPE     = "type";
    private static final String TYPE_ACCESS    = "access";
    private static final String TYPE_REFRESH   = "refresh";

    private final JwtEncoder          jwtEncoder;
    private final JwtDecoder          jwtDecoder;
    private final ITokenRedisService  tokenRedisService;

    @Value("${jwt.token.creator}")
    private String issuer;

    @Value("${jwt.token.access.valid.time}")
    private long accessTtlSec;

    @Value("${jwt.token.refresh.valid.time}")
    private long refreshTtlSec;

    @Value("${jwt.token.access.name}")
    private String accessCookieName;

    @Value("${jwt.token.refresh.name}")
    private String refreshCookieName;

    // ── 토큰 인코딩 ───────────────────────────────────────────────────────────

    /**
     * JWT 인코딩 공통 메서드
     * jti(UUID) 추가 — 블랙리스트 등록 시 식별자로 사용
     */
    private String encode(UserInfoDTO user, long ttlSec, String type) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())              // jti — 블랙리스트 키
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSec))
                .subject(String.valueOf(user.userId()))        // Integer → String
                .claim(CLAIM_USERNAME, user.userName())
                .claim(CLAIM_TYPE, type)
                .claim(CLAIM_ROLES, splitRoles(user.roles()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public String generateAccessToken(UserInfoDTO user) {
        return encode(user, accessTtlSec, TYPE_ACCESS);
    }

    @Override
    public String generateRefreshToken(UserInfoDTO user) {
        return encode(user, refreshTtlSec, TYPE_REFRESH);
    }

    // ── 쿠키 쓰기 ─────────────────────────────────────────────────────────────

    @Override
    public void writeTokensAsCookies(HttpServletResponse res, String accessToken, String refreshToken) {
        res.addHeader("Set-Cookie", buildCookie(accessCookieName,  accessToken,  (int) accessTtlSec).toString());
        res.addHeader("Set-Cookie", buildCookie(refreshCookieName, refreshToken, (int) refreshTtlSec).toString());
    }

    // ── 발급 (로그인) ─────────────────────────────────────────────────────────

    @Override
    public void issueTokens(UserInfoDTO user, HttpServletResponse res) {
        String at = generateAccessToken(user);
        String rt = generateRefreshToken(user);

        // RT → Redis 화이트리스트 저장
        tokenRedisService.saveRefreshToken(user.userId(), rt, refreshTtlSec);
        log.info("issueTokens | RT saved to Redis | userId={}", user.userId());

        writeTokensAsCookies(res, at, rt);
    }

    // ── 폐기 (로그아웃) ───────────────────────────────────────────────────────

    @Override
    public void revokeTokens(HttpServletRequest req, HttpServletResponse res) {

        String atValue = extractCookie(req, accessCookieName);

        if (atValue != null) {
            try {
                Jwt at = jwtDecoder.decode(atValue);

                // AT jti → 블랙리스트 등록 (남은 TTL 계산)
                String jti = at.getId();
                long remaining = at.getExpiresAt() != null
                        ? Instant.now().until(at.getExpiresAt(), ChronoUnit.SECONDS)
                        : 0L;
                if (jti != null) {
                    tokenRedisService.blacklistAccessToken(jti, remaining);
                }

                // RT → Redis 화이트리스트 삭제
                String userIdStr = at.getSubject();
                if (userIdStr != null) {
                    tokenRedisService.deleteRefreshToken(Integer.parseInt(userIdStr));
                }

            } catch (Exception e) {
                // 만료된 토큰으로 로그아웃하는 경우도 있으므로 경고만 기록
                log.warn("revokeTokens | AT 디코딩 또는 Redis 작업 실패 (무시): {}", e.getMessage());
            }
        }

        // 쿠키는 Redis 실패 여부와 관계없이 항상 삭제
        clearCookies(res);
        log.info("revokeTokens | 쿠키 삭제 완료");
    }

    // ── 재발급 (Refresh Token Rotation) ──────────────────────────────────────

    @Override
    public void refreshTokens(HttpServletRequest req, HttpServletResponse res) {

        String rtValue = extractCookie(req, refreshCookieName);
        if (rtValue == null) {
            throw new RuntimeException("Refresh Token 쿠키가 없습니다.");
        }

        // RT 서명·만료 검증
        Jwt rtJwt;
        try {
            rtJwt = jwtDecoder.decode(rtValue);
        } catch (Exception e) {
            throw new RuntimeException("Refresh Token이 유효하지 않습니다.");
        }

        // type 클레임이 "refresh" 인지 확인
        if (!TYPE_REFRESH.equals(rtJwt.getClaimAsString(CLAIM_TYPE))) {
            throw new RuntimeException("Refresh Token 타입이 올바르지 않습니다.");
        }

        Integer userId = Integer.parseInt(rtJwt.getSubject());

        // Redis 화이트리스트 값과 비교
        String storedRt = tokenRedisService.getRefreshToken(userId);
        if (!rtValue.equals(storedRt)) {
            // 이미 사용됐거나 저장된 RT 없음 → 탈취 의심 → 강제 로그아웃
            tokenRedisService.deleteRefreshToken(userId);
            clearCookies(res);
            log.warn("refreshTokens | RT 불일치 — 탈취 의심 | userId={}", userId);
            throw new RuntimeException("Refresh Token 재사용이 감지되었습니다. 다시 로그인해주세요.");
        }

        // 새 AT + RT 발급 (Rotation: 기존 RT 덮어쓰기)
        String userName = rtJwt.getClaimAsString(CLAIM_USERNAME);
        String roles    = joinRoles(rtJwt.getClaimAsStringList(CLAIM_ROLES));

        UserInfoDTO user = UserInfoDTO.builder()
                .userId(userId)
                .userName(userName)
                .roles(roles)
                .build();

        issueTokens(user, res);
        log.info("refreshTokens | 토큰 재발급 완료 | userId={}", userId);
    }

    // ── 내부 유틸 ─────────────────────────────────────────────────────────────

    private ResponseCookie buildCookie(String name, String value, int maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }

    private void clearCookies(HttpServletResponse res) {
        res.addHeader("Set-Cookie", buildCookie(accessCookieName,  "", 0).toString());
        res.addHeader("Set-Cookie", buildCookie(refreshCookieName, "", 0).toString());
    }

    private static String extractCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static List<String> splitRoles(String roles) {
        if (roles == null || roles.isBlank()) return List.of("ROLE_USER");
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String joinRoles(List<String> roleList) {
        if (roleList == null || roleList.isEmpty()) return "ROLE_USER";
        return String.join(",", roleList);
    }
}
