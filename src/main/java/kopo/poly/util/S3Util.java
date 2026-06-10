package kopo.poly.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Component
public class S3Util {

    private final S3Presigner s3Presigner;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public S3Util(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    /**
     * 냉장고 이미지 S3 저장 파일명 생성
     */
    public String createSavedFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "fridge-images/" + UUID.randomUUID() + extension;
    }

    /**
     * 리뷰 이미지 S3 저장 파일명 생성 (review-images/ 경로 → 퍼블릭 읽기 허용)
     */
    public String createReviewImageFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "review-images/" + UUID.randomUUID() + extension;
    }

    /**
     * 리뷰 이미지 퍼블릭 URL 생성 (Presigned 불필요 — 버킷 정책으로 공개)
     */
    public String getPublicUrl(String savedFilename) {
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + savedFilename;
    }

    /**
     * S3 업로드용 Presigned URL 생성
     */
    public String generatePresignedUrlToUpload(String savedFilename) {
        // 업로드 요청 설정
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(savedFilename)
                .build();

        // Presigned 요청 설정 (유효시간 2분)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(2))
                .putObjectRequest(putObjectRequest)
                .build();

        // URL 생성 및 반환
        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String generatePresignedUrlToDownload(String savedFilename) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(savedFilename)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(2))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}