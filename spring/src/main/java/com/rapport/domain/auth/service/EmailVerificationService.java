package com.rapport.domain.auth.service;

import com.rapport.domain.auth.entity.EmailVerification;
import com.rapport.domain.auth.entity.EmailVerificationRepository;
import com.rapport.global.exception.BusinessException;
import com.rapport.global.exception.ErrorCode;
import com.rapport.global.util.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 10;

    private final EmailVerificationRepository emailVerificationRepository;
    private final EmailService emailService;

    @Transactional
    public void sendVerificationCode(String email) {
        emailVerificationRepository.deleteAllByEmail(email);

        String code = generateCode();
        EmailVerification verification = EmailVerification.create(email, code, EXPIRATION_MINUTES);
        emailVerificationRepository.save(verification);

        emailService.sendVerificationCodeEmail(email, code, EXPIRATION_MINUTES);
        log.info("Verification code sent: email={}", email);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_EXPIRED);
        }

        if (!verification.getCode().equals(code)) {
            throw new BusinessException(ErrorCode.EMAIL_VERIFICATION_INVALID);
        }

        verification.verify();
        log.info("Email verified: email={}", email);
    }

    @Transactional(readOnly = true)
    public void checkVerified(String email) {
        boolean verified = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .map(EmailVerification::isVerified)
                .orElse(false);

        if (!verified) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
