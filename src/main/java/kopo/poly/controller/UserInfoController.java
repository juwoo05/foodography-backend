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
@RequestMapping(value = "/user")
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

        UserInfoDTO pDTO = UserInfoDTO.builder().email(email).build();

        UserInfoDTO rDTO = Optional.ofNullable(userInfoService.getUserEmailExists(pDTO))
                .orElseGet(() -> UserInfoDTO.builder().build());

        log.info("{}.getEmailExists End!", this.getClass().getName());

        return rDTO;
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
                .email(email)
                .phoneNum(phoneNum)
                .password(password)
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

    @GetMapping(value = "login")
    public String login() {
        log.info("{}.user/login Start!", this.getClass().getName());

        log.info("{}.user/login End!", this.getClass().getName());

        return "user/login";
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
                .email(email)
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

    @GetMapping(value = "loginSuccess")
    public String loginSuccess() {
        log.info("{}.user/loginSuccess Start!", this.getClass().getName());

        log.info("{}.user/loginSuccess End!", this.getClass().getName());

        return "user/loginSuccess";
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
