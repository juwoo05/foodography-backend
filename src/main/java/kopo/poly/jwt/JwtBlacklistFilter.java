package kopo.poly.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.poly.service.ITokenRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Access Token 블랙리스트 검증 필터
 *
 * BearerTokenAuthenticationFilter 이후에 실행 — JWT 서명·만료 검증이 끝난 뒤
 * Redis 블랙리스트(BL:AT:{jti})에 등록된 토큰이면 401 반환
 * 로그아웃된 토큰이 만료 전에 재사용되는 것을 차단
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final ITokenRedisService tokenRedisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            String jti = jwt.getId();

            if (jti != null && tokenRedisService.isBlacklisted(jti)) {
                log.warn("JwtBlacklistFilter: 블랙리스트 토큰 접근 차단 | jti={}", jti);
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"Token has been invalidated\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
