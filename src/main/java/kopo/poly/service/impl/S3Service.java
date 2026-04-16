package kopo.poly.service.impl;

import kopo.poly.dto.PresignedUrlDTO;
import kopo.poly.service.IS3Service;
import kopo.poly.util.S3Util;
// import com.example.project.repository.ImageRepository; // DB 저장 필요 시 활성화
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class S3Service implements IS3Service {

    private final S3Util s3Util;
    // private final ImageRepository imageRepository;

    public S3Service(S3Util s3Util) {
        this.s3Util = s3Util;
    }

    @Override
    public PresignedUrlDTO getPresignedUrl(String originalFilename) {

        log.info("{}.getPresignedUrl Start!", this.getClass().getName());

        // 1. 고유 파일명 생성 로직 호출 (Util)
        String savedFilename = s3Util.createSavedFilename(originalFilename);

        // 2. Presigned URL 발급 로직 호출 (Util)
        String url = s3Util.generatePresignedUrl(savedFilename);

        // [선택사항] 3. Repository를 통해 DB에 파일 정보 사전 저장 (예: 상태를 '업로드 대기'로 저장)
        // ImageEntity entity = new ImageEntity(savedFilename, originalFilename, "PENDING");
        // imageRepository.save(entity);

        log.info("{}.getPresignedUrl End!", this.getClass().getName());

        // 4. Record DTO로 응답 반환
        return new PresignedUrlDTO(url, savedFilename);
    }
}