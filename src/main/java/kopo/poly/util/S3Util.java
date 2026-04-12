package kopo.poly.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
     * S3에 저장될 고유 파일명 생성
     */
    public String createSavedFilename(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return "fridge-images/" + UUID.randomUUID() + extension;
    }

    /**
     * S3 업로드용 Presigned URL 생성
     */
    public String generatePresignedUrl(String savedFilename) {
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
}