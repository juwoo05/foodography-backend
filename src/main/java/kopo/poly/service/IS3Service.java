package kopo.poly.service;

import kopo.poly.dto.PresignedUrlResponse;

public interface IS3Service {
    PresignedUrlResponse getPresignedUrl(String originalFilename);
}
