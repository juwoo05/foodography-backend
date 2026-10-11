package kopo.poly.service.impl;

import kopo.poly.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService implements ITokenRedisService {

    // Redis 키 prefix
    private static final String RT_PREFIX    = "RT:";       // Refresh Token Whitelist
    private static final String BL_AT_PREFIX = "BL:AT:";   // Access Token Blacklist

    private final StringRedisTemplate stringRedisTemplate;

    // ── Refresh Token Whitelist ────────────────────────────────────────────────

    @Override
    public void saveRefreshToken(Integer userId, String token, long ttlSeconds) {
        log.info("saveRefreshToken | userId={}", userId);
        stringRedisTemplate.opsForValue()
                .set(RT_PREFIX + userId, token, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public String getRefreshToken(Integer userId) {
        return stringRedisTemplate.opsForValue().get(RT_PREFIX + userId);
    }

    @Override
    public void deleteRefreshToken(Integer userId) {
        log.info("deleteRefreshToken | userId={}", userId);
        stringRedisTemplate.delete(RT_PREFIX + userId);
    }

    // ── Access Token Blacklist ─────────────────────────────────────────────────

    @Override
    public void blacklistAccessToken(String jti, long remainingSeconds) {
        if (remainingSeconds <= 0) return;
        log.info("blacklistAccessToken | jti={}, ttl={}s", jti, remainingSeconds);
        stringRedisTemplate.opsForValue()
                .set(BL_AT_PREFIX + jti, "1", Duration.ofSeconds(remainingSeconds));
    }

    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BL_AT_PREFIX + jti));
    }
}
