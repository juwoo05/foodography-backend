package kopo.poly.service;

import kopo.poly.dto.PresignedUrlDTO;

public interface IS3Service {
    PresignedUrlDTO getPresignedUrlToUpload(String originalFilename);

    PresignedUrlDTO getPresignedUrlToDownload(String savedFilename);
}
