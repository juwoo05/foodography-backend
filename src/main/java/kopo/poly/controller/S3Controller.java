package kopo.poly.controller;

import kopo.poly.dto.PresignedUrlResponse;
import kopo.poly.service.IS3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/my-fridge-input")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestParam String filename) {

        log.info("{}.getPresignedUrl Start!", this.getClass().getName());

        PresignedUrlResponse response = s3Service.getPresignedUrl(filename);

        log.info("{}.getPresignedUrl End!", this.getClass().getName());

        return ResponseEntity.ok(response);
    }
}