package kopo.poly.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

public class CookieOrHeaderBearerTokenResolver implements BearerTokenResolver {

    private final String cookieName;

    private final DefaultBearerTokenResolver delegate;

    public CookieOrHeaderBearerTokenResolver(String cookieName) {
        this.cookieName = cookieName;

        this.delegate = new DefaultBearerTokenResolver();

        this.delegate.setAllowFormEncodedBodyParameter(false);
        this.delegate.setAllowUriQueryParameter(false);
    }

    @Override
    public String resolve(HttpServletRequest request) {

        String fromCookie = extractFromCookie(request);
        if (fromCookie != null && !fromCookie.isEmpty()) {
            return fromCookie;
        }

        return delegate.resolve(request);
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie c : cookies) {
            if (cookieName.equals(c.getName())) {
                String y = c.getValue();
                if (y == null) return null;

                y = y.trim();
                if (y.isEmpty()) return null;

                if ((y.startsWith("\"") && y.endsWith("\"")) || (y.startsWith("'") && y.endsWith("'"))) {
                    y = y.substring(1, y.length() - 1).trim();
                }

                // 쿠키 값에 "Bearer " 접두사가 포함된 경우 제거 (방어적 처리)
                final String BEARER_PREFIX = "bearer ";
                if (y.length() > BEARER_PREFIX.length()
                        && y.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
                    y = y.substring(BEARER_PREFIX.length()).trim();
                }

                return y.isEmpty() ? null : y;
            }
        }
        return null;
    }
}
