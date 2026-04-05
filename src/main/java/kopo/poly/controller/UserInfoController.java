package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kopo.poly.dto.MsgDTO;
import kopo.poly.dto.UserInfoDTO;
import kopo.poly.service.IUserInfoService;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping(value = "userRegForm")
    public String userRegForm() {
        log.info("{}.user/userRegForm Start!", this.getClass().getName());

        log.info("{}.user/userRegForm End!", this.getClass().getName());

        return "user/userRegForm";
    }

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
    public MsgDTO sendEmailAuthCode(HttpServletRequest request,
                                    HttpSession session) throws Exception {

        log.info("{}.sendEmailAuthCode Start!", this.getClass().getName());

        String msg;

        String email = CmmUtil.nvl(request.getParameter("email"));

        log.info("email : {}", email);

        UserInfoDTO pDTO = UserInfoDTO.builder().email(EncryptUtil.encAES128CBC(email)).build();

        int res = userInfoService.sendEmailAuthCode(pDTO, session);

        if (res == 1) {
            msg = "발송된 6자리 코드를 입력해주세요.";
        } else {
            msg = "오류로 인해 인증 메일이 발송되지 않았습니다.";
        }

        MsgDTO dto = MsgDTO.builder().result(res).msg(msg).build();

        log.info("{}.sendEmailAuthCode End!", this.getClass().getName());

        return dto;
    }

    @ResponseBody
    @PostMapping("verifyEmailCode")
    public MsgDTO verifyEmailCode(HttpServletRequest request,
                                  HttpSession session) throws Exception {

        log.info("{}.verifyEmailCode Start!", this.getClass().getName());

        String inputCode  = CmmUtil.nvl(request.getParameter("code"));
        String inputEmail = CmmUtil.nvl(request.getParameter("email"));

        String savedCode  = CmmUtil.nvl((String) session.getAttribute("AUTH_NUMBER"));
        String savedEmail = CmmUtil.nvl((String) session.getAttribute("AUTH_EMAIL"));

        int res;
        String msg;

        if (!savedCode.isEmpty()
                && savedCode.equals(inputCode)
                && savedEmail.equals(inputEmail)) {
            res = 1;
            msg = "인증 성공";
            session.removeAttribute("AUTH_NUMBER");
            session.removeAttribute("AUTH_EMAIL");
        } else {
            res = 0;
            msg = "인증번호가 올바르지 않습니다";
        }

        MsgDTO dto = MsgDTO.builder().result(res).msg(msg).build();

        log.info("{}.verifyEmailCode End!", this.getClass().getName());

        return dto;
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

    @ResponseBody
    @PostMapping(value = "loginProc")
    public MsgDTO loginProc(HttpServletRequest request, HttpSession session) throws Exception {

        log.info("{}.loginProc Start!", this.getClass().getName());

        String msg;

        String email = CmmUtil.nvl(request.getParameter("email"));
        String password = CmmUtil.nvl(request.getParameter("password"));

        log.info("email : {}, password : {}", email, password);

        UserInfoDTO pDTO = UserInfoDTO.builder()
                .email(EncryptUtil.encAES128CBC(email))
                .password(EncryptUtil.encHashSHA256(password)).build();

        int res = userInfoService.getUserLogin(pDTO);

        log.info("res : {}", res);

        if (res == 1) {
            msg = "로그인 성공했습니다.";
            session.setAttribute("SS_EMAIL", email);
        } else {
            msg = "이메일과 비밀번호가 올바르지 않습니다";
        }

        MsgDTO dto = MsgDTO.builder().result(res).msg(msg).build();
        log.info("{}.loginProc End!", this.getClass().getName());

        return dto;
    }

    @ResponseBody
    @GetMapping("sessionCheck")
    public UserInfoDTO sessionCheck(HttpSession session) {

        log.info("{}.sessionCheck Start!", this.getClass().getName());

        String email = CmmUtil.nvl(
                (String) session.getAttribute("SS_EMAIL")
        );

        if (!email.isEmpty()) {
            log.info("{}.sessionCheck Yes!", this.getClass().getName());

            return UserInfoDTO.builder()
                    .email(email)
                    .existYn("Y")
                    .build();
        }

        log.info("{}.sessionCheck Null!", this.getClass().getName());

        return UserInfoDTO.builder()
                .existYn("N")
                .build();
    }

    @ResponseBody
    @PostMapping(value = "logout")
    public MsgDTO logout(HttpSession session) {

        log.info("{}.logout Start!", this.getClass().getName());

        session.setAttribute("SS_EMAIL", "");
        session.removeAttribute("SS_EMAIL");

        MsgDTO dto = MsgDTO.builder().result(1).msg("로그아웃하였습니다").build();

        log.info("{}.logout End!", this.getClass().getName());

        return dto;
    }
}
