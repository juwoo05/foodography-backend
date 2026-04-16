package kopo.poly.service;

import kopo.poly.dto.PresignedUrlDTO;

public interface IS3Service {
    PresignedUrlDTO getPresignedUrl(String originalFilename);
}
