package com.rapport.domain.counselor.controller;

import com.rapport.domain.counselor.entity.CounselorCredential;
import com.rapport.domain.counselor.service.CounselorCredentialService;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Counselor Credentials", description = "상담사 자격 서류 업로드/조회 API")
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CounselorCredentialController {

    private final CounselorCredentialService credentialService;

    /**
     * 상담사: PDF 서류 업로드
     * multipart/form-data 로 파일 + 서류 종류 전달
     */
    @Operation(summary = "자격 서류 업로드 (상담사)",
               description = "PDF/이미지 파일을 S3에 업로드합니다. type: LICENSE | DEGREE | CERT | OTHER")
    @PostMapping(value = "/api/v1/counselor/credentials",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<Void>> uploadCredential(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("type") CounselorCredential.CredentialType type,
            @RequestPart("file") MultipartFile file) {

        credentialService.uploadCredential(principal.getId(), type, file);
        return ResponseEntity.ok(ApiResponse.ok("서류가 업로드되었습니다. 관리자 검토 후 결과를 안내드립니다."));
    }

    /**
     * 상담사: 본인이 제출한 서류 목록 조회
     */
    @Operation(summary = "내 제출 서류 목록 (상담사)")
    @GetMapping("/api/v1/counselor/credentials")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<ApiResponse<List<CounselorCredential>>> getMyCredentials(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.ok(credentialService.getMyCredentials(principal.getId())));
    }

    /**
     * 관리자: 특정 상담사의 서류 목록 + 임시 열람 URL 조회
     * URL은 10분간 유효
     */
    @Operation(summary = "상담사 서류 조회 (관리자)",
               description = "서류 열람용 Presigned URL(10분 유효)을 포함하여 반환합니다.")
    @GetMapping("/api/v1/admin/counselors/{userId}/credentials")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CounselorCredentialService.CredentialWithUrlDto>>> getCredentialsForAdmin(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok(credentialService.getCredentialsForAdmin(userId)));
    }
}
