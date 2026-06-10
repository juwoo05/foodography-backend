package kopo.poly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kopo.poly.dto.PresignedUrlDTO;
import kopo.poly.service.IS3Service;
import kopo.poly.util.CmmUtil;
import kopo.poly.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/images")
public class S3Controller {

    private final IS3Service s3Service;
    private final S3Util    s3Util;

    /** 냉장고 이미지 업로드용 Presigned URL */
    @ResponseBody
    @GetMapping("/my-fridge-input")
    public ResponseEntity<PresignedUrlDTO> getPresignedUrlToUpload(HttpServletRequest request) {

        log.info("{}.getPresignedUrlToUpload Start!", this.getClass().getName());

        String filename = CmmUtil.nvl(request.getParameter("filename"));
        PresignedUrlDTO rDTO = s3Service.getPresignedUrlToUpload(filename);

        log.info("{}.getPresignedUrlToUpload End!", this.getClass().getName());
        return ResponseEntity.ok(rDTO);
    }

    /**
     * 리뷰 이미지 업로드용 Presigned URL + 퍼블릭 접근 URL
     * GET /api/images/review-upload?filename=xxx.jpg
     * 응답: { uploadUrl, publicUrl, s3Key }
     */
    @ResponseBody
    @GetMapping("/review-upload")
    public ResponseEntity<Map<String, String>> getReviewImageUploadUrl(HttpServletRequest request) {

        log.info("{}.getReviewImageUploadUrl Start!", this.getClass().getName());

        String filename   = CmmUtil.nvl(request.getParameter("filename"));
        String s3Key      = s3Util.createReviewImageFilename(filename);
        String uploadUrl  = s3Util.generatePresignedUrlToUpload(s3Key);
        String publicUrl  = s3Util.getPublicUrl(s3Key);

        log.info("{}.getReviewImageUploadUrl End! s3Key={}", this.getClass().getName(), s3Key);

        return ResponseEntity.ok(Map.of(
                "uploadUrl", uploadUrl,
                "publicUrl", publicUrl,
                "s3Key",     s3Key
        ));
    }
}