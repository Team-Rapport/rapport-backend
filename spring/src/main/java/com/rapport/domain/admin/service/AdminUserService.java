package com.rapport.domain.admin.service;

import com.rapport.domain.admin.dto.AdminUserDto;
import com.rapport.domain.auth.entity.RefreshTokenRepository;
import com.rapport.domain.user.entity.User;
import com.rapport.domain.user.entity.UserRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public Page<AdminUserDto.UserSummary> getUsers(User.Role role, String search, Pageable pageable) {
        return userRepository.searchUsers(role, search, pageable)
                .map(this::toSummary);
    }

    @Transactional
    public AdminUserDto.ToggleResponse deactivate(Long userId) {
        User user = findUserOrThrow(userId);

        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_DEACTIVATED);
        }

        user.deactivate();
        refreshTokenRepository.deleteAllByUserId(userId);
        log.info("Admin deactivated user: userId={}", userId);

        return AdminUserDto.ToggleResponse.builder()
                .userId(userId)
                .isActive(false)
                .message("계정이 비활성화되었습니다.")
                .build();
    }

    @Transactional
    public AdminUserDto.ToggleResponse activate(Long userId) {
        User user = findUserOrThrow(userId);

        if (user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_ALREADY_ACTIVE);
        }

        user.activate();
        log.info("Admin activated user: userId={}", userId);

        return AdminUserDto.ToggleResponse.builder()
                .userId(userId)
                .isActive(true)
                .message("계정이 활성화되었습니다.")
                .build();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private AdminUserDto.UserSummary toSummary(User user) {
        return AdminUserDto.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .isActive(user.isActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
