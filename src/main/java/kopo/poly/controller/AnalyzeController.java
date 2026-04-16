package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.util.CmmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/api")
public class AnalyzeController {

    @PostMapping("/analyze")
    public String analyze(HttpServletRequest request) {

        log.info("{}.verifyEmailCode Start!", this.getClass().getName());

        String s3Key = CmmUtil.nvl(request.getParameter("s3Key"));

        log.info("{}.verifyEmailCode End!", this.getClass().getName());

        return null;
    }
}
