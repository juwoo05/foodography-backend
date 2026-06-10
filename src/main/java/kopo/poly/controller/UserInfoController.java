package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kopo.poly.auth.AuthInfo;

import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IJwtTokenService;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Slf4j
@RequestMapping(value = "/api/user")
@RequiredArgsConstructor
@Controller
public class UserInfoController {

    private final IUserInfoService userInfoService;
    private final AuthenticationManager authenticationManager;
    private final IJwtTokenService jwtTokenService;

    @ResponseBody
    @PostMapping(value = "getEmailExists")
    public UserInfoDTO getUserExist(HttpServletRequest request) throws Exception {

        log.info("{}.getEmailExists Start!", this.getClass().getName());

        String email = CmmUtil.nvl(request.getParameter("email"));

        log.info("email : {}", email);

        UserInfoDTO pDTO = UserInfoDTO.builder().email(EncryptUtil.encAES128CBC(email)).build();

        UserInfoDTO rDTO = Optional.ofNullable(userInfoService.getUserEmailExists(pDTO))
                .orElseGet(() -> UserInfoDTO.builder().build());

        log.info("{}.getEmailExists End!", this.getClass().getName());

        return rDTO;
    }

    @ResponseBody
    @PostMapping("sendEmailAuthCode")
    public MsgDTO sendEmailAuthCode(HttpServletRequest request) throws Exception {

        log.info("{}.sendEmailAuthCode Start!", this.getClass().getName());

        String email = CmmUtil.nvl(request.getParameter("email"));

        log.info("email : {}", email);

        UserInfoDTO pDTO = UserInfoDTO.builder().email(EncryptUtil.encAES128CBC(email)).build();

        int res = userInfoService.sendEmailAuthCode(pDTO);    // HttpSession 제거

        String msg = (res == 1) ? "발송된 6자리 코드를 입력해주세요." : "오류로 인해 인증 메일이 발송되지 않았습니다.";

        log.info("{}.sendEmailAuthCode End!", this.getClass().getName());

        return MsgDTO.builder().result(res).msg(msg).build();
    }

    @ResponseBody
    @PostMapping("verifyEmailCode")
    public MsgDTO verifyEmailCode(HttpServletRequest request) {

        log.info("{}.verifyEmailCode Start!", this.getClass().getName());

        String inputCode  = CmmUtil.nvl(request.getParameter("code"));
        String inputEmail = CmmUtil.nvl(request.getParameter("email"));

        // 비즈니스 로직(Redis 조회·비교·삭제)은 서비스 계층에서 처리
        int    res = userInfoService.verifyEmailCode(inputEmail, inputCode);
        String msg = (res == 1) ? "인증 성공" : "인증번호가 올바르지 않습니다";

        log.info("{}.verifyEmailCode End!", this.getClass().getName());

        return MsgDTO.builder().result(res).msg(msg).build();
    }

    @ResponseBody
    @PostMapping(value = "insertUserInfo")
    public MsgDTO insertUserInfo(HttpServletRequest request) throws Exception {

        log.info("{}.insertUserInfo start!", this.getClass().getName());

        String msg;

        String userName = CmmUtil.nvl(request.getParameter("userName"));
        String email = CmmUtil.nvl(request.getParameter("email"));
        String phoneNum = CmmUtil.nvl(request.getParameter("phoneNum"));
        String password = CmmUtil.nvl(request.getParameter("password"));

        log.info("userName : {}, email : {}, phoneNum : {}, password : {}",
                userName, email, phoneNum, password);

        UserInfoDTO pDTO = UserInfoDTO.builder()
                .userName(userName)
                .email(EncryptUtil.encAES128CBC(email))
                .phoneNum(phoneNum)
                .password(EncryptUtil.encHashSHA256(password))
                .build();

        int res = userInfoService.insertUserInfo(pDTO);

        log.info("회원가입 결과(res) : {}", res);

        if (res == 1) {
            msg = "회원가입되었습니다.";
        } else if (res == 2) {
            msg = "이미 가입된 아이디입니다,";
        } else {
            msg = "오류로 인해 회원가입이 실패하였습니다.";
        }

        MsgDTO dto = MsgDTO.builder().result(res).msg(msg).build();

        log.info("{}.insertUserInfo End!", this.getClass().getName());

        return dto;
    }

    /**
     * 로그인 — Spring Security AuthenticationManager 를 통한 인증 후 JWT 쿠키 발급
     *
     * 흐름:
     *   1. authenticationManager.authenticate() 호출
     *   2. 내부에서 UserDetailsService.loadUserByUsername(encryptedEmail) 호출
     *   3. PasswordEncoder.matches(plainPw, sha256Hash) 비교
     *   4. 성공 시 principal(AuthInfo) 에서 UserInfoDTO 추출 → JWT 쿠키 발급
     */
    @ResponseBody
    @PostMapping(value = "loginProc")
    public MsgDTO loginProc(HttpServletRequest request, HttpServletResponse response) throws Exception {

        log.info("{}.loginProc Start!", this.getClass().getName());

        String email    = CmmUtil.nvl(request.getParameter("email"));
        String password = CmmUtil.nvl(request.getParameter("password"));

        log.info("email : {}", email);

        try {
            // AES-128 암호화 이메일 = DB 저장 키와 동일한 형태로 전달
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            EncryptUtil.encAES128CBC(email),
                            password   // 평문 — PasswordEncoder.matches() 에서 SHA-256 비교
                    )
            );

            // 인증 성공 → JWT 쿠키 발급
            AuthInfo authInfo = (AuthInfo) authentication.getPrincipal();
            jwtTokenService.issueTokens(authInfo.userInfoDTO(), response);

            log.info("{}.loginProc Success! userId={}", this.getClass().getName(),
                    authInfo.userInfoDTO().userId());

            return MsgDTO.builder().result(1).msg("로그인 성공했습니다.").build();

        } catch (AuthenticationException e) {
            log.warn("{}.loginProc Failed: {}", this.getClass().getName(), e.getMessage());
            return MsgDTO.builder().result(0).msg("이메일과 비밀번호가 올바르지 않습니다").build();
        }
    }

    /**
     * 토큰 유효성 확인 — JWT 가 유효하면 SecurityContextHolder 에서 클레임 추출
     * 유효하지 않으면 oauth2ResourceServer 필터가 먼저 401 을 반환하므로
     * 이 메서드에 도달했다면 항상 인증된 상태
     */
    @ResponseBody
    @GetMapping("sessionCheck")
    public UserInfoDTO sessionCheck() {

        log.info("{}.sessionCheck Start!", this.getClass().getName());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            UserInfoDTO rDTO = UserInfoDTO.builder()
                    .userId(Integer.parseInt(jwt.getSubject()))
                    .userName(jwt.getClaimAsString("username"))
                    .existYn("Y")
                    .build();
            log.info("{}.sessionCheck Valid! userId={}", this.getClass().getName(), rDTO.userId());
            return rDTO;
        }

        log.info("{}.sessionCheck Invalid!", this.getClass().getName());
        return UserInfoDTO.builder().existYn("N").build();
    }

    /**
     * POST /api/user/refresh
     * Refresh Token 으로 Access Token + Refresh Token 재발급 (Rotation)
     *
     * AT 만료 후 클라이언트가 자동 호출 — permitAll 이므로 만료된 AT 없이도 접근 가능
     * RT 검증은 JwtTokenService 에서 처리 (서명 + Redis 화이트리스트 비교)
     */
    @ResponseBody
    @PostMapping("refresh")
    public MsgDTO refresh(HttpServletRequest request, HttpServletResponse response) {
        log.info("{}.refresh Start!", this.getClass().getName());
        try {
            jwtTokenService.refreshTokens(request, response);
            log.info("{}.refresh Success!", this.getClass().getName());
            return MsgDTO.builder().result(1).msg("토큰이 갱신되었습니다.").build();
        } catch (Exception e) {
            log.warn("{}.refresh Failed: {}", this.getClass().getName(), e.getMessage());
            return MsgDTO.builder().result(0).msg(e.getMessage()).build();
        }
    }

    // logout 은 SecurityConfig 의 Spring Security LogoutFilter 가 처리
    // POST /api/user/logout → AT 블랙리스트 + RT 삭제 + 쿠키 만료 + JSON 반환

    @ResponseBody
    @PostMapping(value = "searchUserEmail")
    public UserInfoDTO searchUserEmail(HttpServletRequest request) throws Exception{

        log.info("{}.searchUserId Start!", this.getClass().getName());

        String userName = CmmUtil.nvl(request.getParameter("userName"));
        String phoneNum = CmmUtil.nvl(request.getParameter("phoneNum"));

        UserInfoDTO pDTO = UserInfoDTO.builder()
                .userName(userName)
                .phoneNum(phoneNum)
                .build();

        UserInfoDTO rDTO = Optional.ofNullable(userInfoService.searchUserEmail(pDTO))
                .orElseGet(() -> UserInfoDTO.builder().build());

        log.info("{}.searchUserId End!", this.getClass().getName());

        return rDTO;
    }

    @ResponseBody
    @PostMapping("updatePassword")
    public MsgDTO updatePassword(HttpServletRequest request) throws Exception {

        log.info("{}.updatePassword Start!", this.getClass().getName());

        String msg;

        String email    = CmmUtil.nvl(request.getParameter("email"));
        String password = CmmUtil.nvl(request.getParameter("password"));

        log.info("updatePassword email : {}", email);

        UserInfoDTO pDTO = UserInfoDTO.builder()
                .email(EncryptUtil.encAES128CBC(email))        // DB 조회용 암호화
                .password(EncryptUtil.encHashSHA256(password)) // 비밀번호 해시
                .build();

        int res = userInfoService.updatePassword(pDTO);

        if (res == 1) {
            msg = "비밀번호가 성공적으로 변경되었습니다.";
        } else if (res == 2) {
            msg = "새 비밀번호가 기존 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요.";
        } else {
            msg = "오류로 인해 비밀번호 변경에 실패했습니다.";
        }

        log.info("{}.updatePassword End!", this.getClass().getName());

        return MsgDTO.builder().result(res).msg(msg).build();
    }
}
