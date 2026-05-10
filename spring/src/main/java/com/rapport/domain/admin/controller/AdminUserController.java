package com.rapport.domain.admin.controller;

import com.rapport.domain.admin.dto.AdminUserDto;
import com.rapport.domain.admin.service.AdminUserService;
import com.rapport.domain.user.entity.User;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - User", description = "관리자 회원 관리 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "회원 목록 조회",
               description = "역할별 필터 및 이름/이메일 검색. role 미입력 시 전체 조회.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserDto.UserSummary>>> getUsers(
            @Parameter(description = "역할 필터 (CLIENT, COUNSELOR, ADMIN)")
            @RequestParam(required = false) User.Role role,
            @Parameter(description = "이름 또는 이메일 검색어")
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(adminUserService.getUsers(role, search, pageable)));
    }

    @Operation(summary = "회원 계정 상태 변경", description = "active=true면 활성화, false면 비활성화합니다.")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserDto.ToggleResponse>> toggleStatus(
            @PathVariable Long userId,
            @RequestBody StatusRequest request) {
        return request.isActive()
                ? ResponseEntity.ok(ApiResponse.ok("계정이 활성화되었습니다.", adminUserService.activate(userId)))
                : ResponseEntity.ok(ApiResponse.ok("계정이 비활성화되었습니다.", adminUserService.deactivate(userId)));
    }

    @Operation(summary = "회원 계정 비활성화", description = "지정한 회원의 계정을 비활성화하고 토큰을 만료시킵니다.")
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse<AdminUserDto.ToggleResponse>> deactivate(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok("계정이 비활성화되었습니다.", adminUserService.deactivate(userId)));
    }

    @Operation(summary = "회원 계정 활성화", description = "비활성화된 회원 계정을 다시 활성화합니다.")
    @PatchMapping("/{userId}/activate")
    public ResponseEntity<ApiResponse<AdminUserDto.ToggleResponse>> activate(
            @PathVariable Long userId) {
        return ResponseEntity.ok(
                ApiResponse.ok("계정이 활성화되었습니다.", adminUserService.activate(userId)));
    }

    @Getter
    static class StatusRequest {
        private boolean active;
    }
}
