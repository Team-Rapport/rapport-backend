package com.rapport.global.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 허용 파일 타입
    private static final List<String> ALLOWED_CONTENT_TYPES =
            List.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 파일 업로드
     * @param file      업로드할 파일
     * @param folder    S3 저장 폴더 (예: "credentials", "profiles")
     * @return          S3 URL
     */
    public String upload(MultipartFile file, String folder) {
        validateFile(file);

        String fileName = folder + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.Private)); // Private: URL로만 접근
        } catch (IOException e) {
            log.error("S3 upload failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        String url = amazonS3.getUrl(bucket, fileName).toString();
        log.info("S3 upload success: {}", url);
        return url;
    }

    /**
     * 파일 삭제
     * @param fileUrl   삭제할 S3 URL
     */
    public void delete(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, key));
            log.info("S3 delete success: {}", key);
        } catch (Exception e) {
            log.warn("S3 delete failed (무시하고 계속): {}", e.getMessage());
        }
    }

    /**
     * Presigned URL 발급 (일정 시간 동안 파일 열람 가능한 임시 URL)
     * 관리자가 PDF를 열람할 때 사용
     * @param fileUrl   S3 URL
     * @param minutes   유효 시간 (분)
     */
    public String generatePresignedUrl(String fileUrl, int minutes) {
        String key = extractKeyFromUrl(fileUrl);
        java.util.Date expiration = new java.util.Date(
                System.currentTimeMillis() + (long) minutes * 60 * 1000);
        return amazonS3.generatePresignedUrl(bucket, key, expiration).toString();
    }

    // ===== private =====

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "파일이 비어있습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "파일 크기는 10MB를 초과할 수 없습니다.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "PDF, JPG, PNG 파일만 업로드 가능합니다.");
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        // https://bucket.s3.region.amazonaws.com/folder/filename.pdf
        // → folder/filename.pdf
        return fileUrl.substring(fileUrl.indexOf(".amazonaws.com/") + ".amazonaws.com/".length());
    }
}
