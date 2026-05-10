package com.rapport.domain.admin.dto;

import com.rapport.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AdminUserDto {

    @Getter
    @Builder
    public static class UserSummary {
        private Long id;
        private String email;
        private String name;
        private User.Role role;
        private boolean isActive;
        private LocalDateTime lastLoginAt;
        private LocalDateTime createdAt;
    }

    @Getter
    @Builder
    public static class ToggleResponse {
        private Long userId;
        private boolean isActive;
        private String message;
    }
}
