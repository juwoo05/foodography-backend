package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.PresignedUrlDTO;
import kopo.poly.service.IS3Service;
import kopo.poly.util.CmmUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class S3Controller {

    // 인터페이스(IService)에 의존
    private final IS3Service s3Service;

    public S3Controller(IS3Service s3Service) {
        this.s3Service = s3Service;
    }

    @ResponseBody
    @GetMapping("/my-fridge-input")
    public ResponseEntity<PresignedUrlDTO> getPresignedUrlToUpload(HttpServletRequest request) {

        log.info("{}.getPresignedUrl Start!", this.getClass().getName());

        String filename = CmmUtil.nvl(request.getParameter("filename"));

        PresignedUrlDTO rDTO = s3Service.getPresignedUrlToUpload(filename);

        log.info("presignedUrlToUpload: {}", rDTO);

        log.info("{}.getPresignedUrl End!", this.getClass().getName());

        return ResponseEntity.ok(rDTO);
    }

    @ResponseBody
    @GetMapping("/my-fridge-download")
    public ResponseEntity<PresignedUrlDTO> getPresignedUrlToDownload(HttpServletRequest request) {

        log.info("{}.getPresignedUrlToDownload Start!", this.getClass().getName());

        // DB에서 가져온 savedFilename을 파라미터로 받음
        String savedFilename = CmmUtil.nvl(request.getParameter("filename"));

        PresignedUrlDTO rDTO = s3Service.getPresignedUrlToDownload(savedFilename);

        log.info("PresignedUrlToDownload: {}", rDTO);

        log.info("{}.getPresignedUrlToDownload End!", this.getClass().getName());

        return ResponseEntity.ok(rDTO);
    }
}