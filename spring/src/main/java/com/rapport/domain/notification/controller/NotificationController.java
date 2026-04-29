package com.rapport.domain.notification.controller;

import com.rapport.domain.notification.entity.Notification;
import com.rapport.domain.notification.entity.NotificationRepository;
import com.rapport.global.config.UserPrincipal;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Notification", description = "알림 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @Operation(summary = "알림 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<NotificationResponse> result = notificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(principal.getId(), pageable)
                .map(NotificationResponse::from);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "읽지 않은 알림 수 조회")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @Operation(summary = "알림 읽음 처리 (단건)")
    @PatchMapping("/{notificationId}/read")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(n -> n.getUser().getId().equals(principal.getId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        notification.markRead();
        return ResponseEntity.ok(ApiResponse.ok("알림을 읽음 처리했습니다."));
    }

    @Operation(summary = "전체 알림 읽음 처리")
    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationRepository.findAllByUserIdOrderByCreatedAtDesc(
                        principal.getId(), Pageable.unpaged())
                .forEach(Notification::markRead);
        return ResponseEntity.ok(ApiResponse.ok("모든 알림을 읽음 처리했습니다."));
    }

    @Getter @Builder
    static class NotificationResponse {
        private Long id;
        private Notification.NotificationType type;
        private String title;
        private String body;
        private String referenceType;
        private Long referenceId;
        private boolean isRead;
        private LocalDateTime createdAt;

        static NotificationResponse from(Notification n) {
            return NotificationResponse.builder()
                    .id(n.getId())
                    .type(n.getType())
                    .title(n.getTitle())
                    .body(n.getBody())
                    .referenceType(n.getReferenceType())
                    .referenceId(n.getReferenceId())
                    .isRead(n.isRead())
                    .createdAt(n.getCreatedAt())
                    .build();
        }
    }
}
