package kopo.poly.config;

import kopo.poly.jwt.JwtBlacklistFilter;
import kopo.poly.service.IJwtTokenService;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final BearerTokenResolver        bearerTokenResolver;
    private final JwtDecoder                 jwtDecoder;
    private final JwtBlacklistFilter         jwtBlacklistFilter;
    private final IJwtTokenService           jwtTokenService;

    @Value("${jwt.token.access.name}")
    private String accessTokenName;

    @Value("${jwt.token.refresh.name}")
    private String refreshTokenName;

    /**
     * DB에 SHA-256 해시로 저장된 기존 비밀번호와 호환되는 PasswordEncoder
     * AuthenticationManager.authenticate() 내부에서 matches() 가 호출됨
     * BCrypt 로 변경하려면 insertUserInfo 에서도 BCrypt 인코딩으로 함께 바꿔야 함
     */
    /**
     * DB에 SHA-256 해시로 저장된 기존 비밀번호와 호환되는 PasswordEncoder
     * AuthenticationManager.authenticate() 내부에서 matches() 가 호출됨
     * BCrypt 로 변경하려면 insertUserInfo 에서도 BCrypt 인코딩으로 함께 바꿔야 함
     *
     * PasswordEncoder 인터페이스는 checked exception 선언 불가
     * → EncryptUtil 의 checked exception 을 RuntimeException 으로 래핑
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence raw) {
                try {
                    return EncryptUtil.encHashSHA256(raw.toString());
                } catch (Exception e) {
                    throw new RuntimeException("SHA-256 해싱 실패", e);
                }
            }
            @Override
            public boolean matches(CharSequence raw, String encoded) {
                return encode(raw).equals(encoded);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:8080",
                "http://52.79.63.159:8080",
                "https://ridinggoat.kr",
                "http://ridinggoat.kr"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ── 세션 완전 비활성화 ────────────────────────────────────────
                // STATELESS 만으로는 DelegatingSecurityContextRepository 내부의
                // HttpSessionSecurityContextRepository 가 delegate 체인에 남아
                // JSESSIONID 를 발급할 수 있음 → NullSecurityContextRepository 로 완전 차단
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(sc -> sc.securityContextRepository(new NullSecurityContextRepository()))

                // ExceptionTranslationFilter 가 사용하는 HttpSessionRequestCache 도 세션을 참조함
                // → NullRequestCache 로 교체하여 세션 참조 경로 완전 제거
                .requestCache(rc -> rc.requestCache(new NullRequestCache()))

                .csrf(AbstractHttpConfigurer::disable)

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .authorizeHttpRequests(auth -> auth

                        // ── 공개 API (인증 불필요) ────────────────────────────
                        // 회원가입 흐름
                        .requestMatchers(
                                "/api/user/getEmailExists",
                                "/api/user/sendEmailAuthCode",
                                "/api/user/verifyEmailCode",
                                "/api/user/insertUserInfo"
                        ).permitAll()

                        // 로그인 / 아이디·비밀번호 찾기 / 토큰 재발급 / 토큰 확인
                        .requestMatchers(
                                "/api/user/loginProc",
                                "/api/user/searchUserEmail",
                                "/api/user/updatePassword",
                                "/api/user/refresh",          // RT로 AT 재발급 — AT 없이 호출
                                "/api/user/token/verify"      // 비로그인 시 200+existYn:N 반환 (401 콘솔 에러 방지)
                        ).permitAll()

                        // 이메일 발송
                        .requestMatchers("/mail/sendMail").permitAll()

                        // 공지사항 조회 (목록·상세) — 비로그인 열람 허용
                        .requestMatchers(HttpMethod.GET, "/api/notice/**").permitAll()

                        // 공지사항 작성·수정·삭제 — 관리자 전용 (roles 클레임 ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST,   "/api/notice", "/api/notice/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/notice/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notice/**").hasRole("ADMIN")

                        // ── 인증 필요 API ─────────────────────────────────────
                        // /api/** 하위는 모두 JWT 검증
                        .requestMatchers("/api/**").authenticated()

                        // ── React SPA 전체 허용 ───────────────────────────────
                        // /api/** 가 아닌 모든 경로(정적 리소스 + React 라우트)는 허용
                        // 페이지 접근 권한은 React Router + authStore 가 클라이언트에서 처리
                        .anyRequest().permitAll()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(bearerTokenResolver)              //쿠키에서 토큰 꺼냄
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)                           //서명 만료 검증
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )

                // AT 블랙리스트 검증 필터 — BearerTokenAuthenticationFilter 이후 실행
                .addFilterAfter(jwtBlacklistFilter, BasicAuthenticationFilter.class)

                .logout(logout -> logout
                        .logoutUrl("/api/user/logout")
                        // Redis 처리: AT 블랙리스트 등록 + RT 삭제 + 쿠키 만료
                        .addLogoutHandler((req, res, auth) -> {
                            try {
                                jwtTokenService.revokeTokens(req, res);
                            } catch (Exception e) {
                                log.warn("logout revokeTokens 실패 (무시): {}", e.getMessage());
                            }
                        })
                        .deleteCookies(accessTokenName, refreshTokenName) // 안전망
                        .logoutSuccessHandler((req, res, auth) -> {
                            res.setStatus(HttpStatus.OK.value());
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            res.getWriter().write("{\"result\":1,\"msg\":\"로그아웃하였습니다\"}");
                        })
                );

        return http.build();
    }
}
