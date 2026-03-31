-- ============================================================
-- Rapport DB Schema v2.0
-- Flyway Migration V1__init_schema.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET time_zone = '+09:00';

-- ============================================================
-- 1. users — 통합 계정
-- ============================================================
CREATE TABLE IF NOT EXISTS `users` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    `email`             VARCHAR(255)        NOT NULL,
    `password_hash`     VARCHAR(255)        NULL COMMENT 'BCrypt; NULL = 소셜 전용 계정',
    `role`              ENUM('CLIENT','COUNSELOR','ADMIN') NOT NULL,
    `name`              VARCHAR(100)        NOT NULL COMMENT 'AES-256 암호화',
    `phone`             VARCHAR(20)         NULL     COMMENT 'AES-256 암호화',
    `profile_image_url` VARCHAR(500)        NULL,
    `is_active`         TINYINT(1)          NOT NULL DEFAULT 1,
    `is_anonymized`     TINYINT(1)          NOT NULL DEFAULT 0 COMMENT 'PII 익명화 배치 처리 플래그',
    `last_login_at`     DATETIME            NULL,
    `created_at`        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        DATETIME            NULL     COMMENT 'Soft Delete',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`),
    INDEX `idx_users_role` (`role`),
    INDEX `idx_users_deleted_at` (`deleted_at`),
    INDEX `idx_users_anonymized` (`is_anonymized`, `deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. oauth_accounts — 소셜 로그인 연동
-- ============================================================
CREATE TABLE IF NOT EXISTS `oauth_accounts` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT UNSIGNED NOT NULL,
    `provider`      ENUM('GOOGLE','KAKAO') NOT NULL,
    `provider_id`   VARCHAR(255)    NOT NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_oauth_provider_id` (`provider`, `provider_id`),
    INDEX `idx_oauth_user_id` (`user_id`),
    CONSTRAINT `fk_oauth_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 3. refresh_tokens — JWT Refresh Token
-- ============================================================
CREATE TABLE IF NOT EXISTS `refresh_tokens` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT UNSIGNED NOT NULL,
    `token`         VARCHAR(512)    NOT NULL,
    `expires_at`    DATETIME        NOT NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token` (`token`),
    INDEX `idx_refresh_user_id` (`user_id`),
    CONSTRAINT `fk_refresh_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 4. counselor_profiles — 상담사 프로필
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_profiles` (
    `id`                BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT UNSIGNED     NOT NULL,
    `license_type`      VARCHAR(100)        NOT NULL,
    `license_number`    VARCHAR(100)        NULL     COMMENT 'AES-256 암호화',
    `counselor_gender`  ENUM('MALE','FEMALE','ANY') NOT NULL DEFAULT 'ANY',
    `specializations`   JSON                NOT NULL,
    `approaches`        JSON                NULL,
    `bio`               TEXT                NULL,
    `experience_years`  TINYINT UNSIGNED    NULL,
    `office_address`    VARCHAR(500)        NULL     COMMENT 'AES-256 암호화',
    `office_lat`        DECIMAL(10,7)       NULL,
    `office_lng`        DECIMAL(10,7)       NULL,
    `approval_status`   ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `rejection_reason`  TEXT                NULL,
    `approved_at`       DATETIME            NULL,
    `is_verified`       TINYINT(1)          NOT NULL DEFAULT 0,
    `average_rating`    DECIMAL(3,2)        NULL,
    `review_count`      INT UNSIGNED        NOT NULL DEFAULT 0,
    `created_at`        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        DATETIME            NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_counselor_user_id` (`user_id`),
    INDEX `idx_counselor_status` (`approval_status`),
    INDEX `idx_counselor_gender` (`counselor_gender`),
    CONSTRAINT `fk_counselor_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 5. counselor_credentials — 자격 서류
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_credentials` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`      BIGINT UNSIGNED NOT NULL,
    `credential_type`   ENUM('LICENSE','DEGREE','CERT','OTHER') NOT NULL,
    `file_url`          VARCHAR(500)    NOT NULL COMMENT 'S3 경로',
    `status`            ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    `reviewed_by`       BIGINT UNSIGNED NULL,
    `reviewed_at`       DATETIME        NULL,
    `rejection_reason`  TEXT            NULL,
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_credentials_counselor` (`counselor_id`),
    INDEX `idx_credentials_status` (`status`),
    CONSTRAINT `fk_credentials_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_credentials_reviewer` FOREIGN KEY (`reviewed_by`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 6. session_types — 상담 유형 마스터
-- ============================================================
CREATE TABLE IF NOT EXISTS `session_types` (
    `id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`  VARCHAR(50)     NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_type_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `session_types` (`name`) VALUES ('CHAT'), ('CALL'), ('VIDEOCALL'), ('MEETING');

-- ============================================================
-- 7. counselor_session_types — 상담사별 유형/가격 매핑
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_session_types` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`    BIGINT UNSIGNED NOT NULL,
    `session_type_id` BIGINT UNSIGNED NOT NULL,
    `price`           INT UNSIGNED    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_counselor_session_type` (`counselor_id`, `session_type_id`),
    CONSTRAINT `fk_cst_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_cst_type` FOREIGN KEY (`session_type_id`) REFERENCES `session_types`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 8. keywords — 상담사 키워드 마스터
-- ============================================================
CREATE TABLE IF NOT EXISTS `keywords` (
    `id`    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `name`  VARCHAR(100)    NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_keyword_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 9. counselor_keywords — 상담사-키워드 N:M 매핑
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_keywords` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`  BIGINT UNSIGNED NOT NULL,
    `keyword_id`    BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_counselor_keyword` (`counselor_id`, `keyword_id`),
    CONSTRAINT `fk_ck_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_ck_keyword` FOREIGN KEY (`keyword_id`) REFERENCES `keywords`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 10. counselor_schedules — 상담사 가용 시간 슬롯
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_schedules` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`    BIGINT UNSIGNED NOT NULL,
    `session_type_id` BIGINT UNSIGNED NOT NULL,
    `slot_date`       DATE            NOT NULL,
    `start_time`      TIME            NOT NULL,
    `end_time`        TIME            NOT NULL,
    `is_available`    TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_schedule_counselor_date` (`counselor_id`, `slot_date`),
    CONSTRAINT `fk_schedule_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_schedule_type` FOREIGN KEY (`session_type_id`) REFERENCES `session_types`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 11. terms — 약관 마스터
-- ============================================================
CREATE TABLE IF NOT EXISTS `terms` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `title`         VARCHAR(200)    NOT NULL,
    `content`       TEXT            NOT NULL,
    `version`       VARCHAR(20)     NOT NULL,
    `is_mandatory`  TINYINT(1)      NOT NULL DEFAULT 1,
    `is_active`     TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 12. user_consents — 약관 동의 이력
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_consents` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT UNSIGNED NOT NULL,
    `term_id`       BIGINT UNSIGNED NOT NULL,
    `is_agreed`     TINYINT(1)      NOT NULL,
    `agreed_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_consent_user` (`user_id`),
    CONSTRAINT `fk_consent_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_consent_term` FOREIGN KEY (`term_id`) REFERENCES `terms`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 13. ai_chat_sessions — AI 챗봇 세션
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_chat_sessions` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `client_id`     BIGINT UNSIGNED NOT NULL,
    `status`        ENUM('IN_PROGRESS','COMPLETED','ABANDONED') NOT NULL DEFAULT 'IN_PROGRESS',
    `consent_agreed` TINYINT(1)     NOT NULL DEFAULT 0,
    `consent_at`    DATETIME        NULL,
    `started_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finished_at`   DATETIME        NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`    DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_chat_session_client` (`client_id`),
    CONSTRAINT `fk_chat_session_client` FOREIGN KEY (`client_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 14. reports — AI 리포트
-- ============================================================
CREATE TABLE IF NOT EXISTS `reports` (
    `id`                        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `session_id`                BIGINT UNSIGNED NOT NULL,
    `client_id`                 BIGINT UNSIGNED NOT NULL,
    `depression_score`          TINYINT UNSIGNED NULL,
    `anxiety_score`             TINYINT UNSIGNED NULL,
    `stress_score`              TINYINT UNSIGNED NULL,
    `risk_level`                ENUM('LOW','MODERATE','HIGH','CRITICAL') NOT NULL DEFAULT 'LOW',
    `summary`                   TEXT            NULL COMMENT 'AES-256 암호화, NER 마스킹',
    `report_keywords`           JSON            NULL,
    `recommended_specializations` JSON          NULL,
    `is_crisis_detected`        TINYINT(1)      NOT NULL DEFAULT 0,
    `created_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`                DATETIME        NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_session` (`session_id`),
    INDEX `idx_report_client` (`client_id`),
    CONSTRAINT `fk_report_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_sessions`(`id`),
    CONSTRAINT `fk_report_client` FOREIGN KEY (`client_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 15. counseling_cases — 장기 상담 관계
-- ============================================================
CREATE TABLE IF NOT EXISTS `counseling_cases` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `client_id`         BIGINT UNSIGNED NOT NULL,
    `counselor_id`      BIGINT UNSIGNED NOT NULL,
    `status`            ENUM('ACTIVE','PAUSED','COMPLETED','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    `started_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_at`          DATETIME        NULL,
    `total_sessions`    INT UNSIGNED    NOT NULL DEFAULT 0,
    `primary_concern`   VARCHAR(500)    NULL COMMENT 'AES-256 암호화',
    `notes`             TEXT            NULL COMMENT 'AES-256 암호화',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_case_client` (`client_id`),
    INDEX `idx_case_counselor` (`counselor_id`),
    CONSTRAINT `fk_case_client` FOREIGN KEY (`client_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_case_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 16. bookings — 예약
-- ============================================================
CREATE TABLE IF NOT EXISTS `bookings` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `case_id`               BIGINT UNSIGNED NULL,
    `client_id`             BIGINT UNSIGNED NOT NULL,
    `counselor_id`          BIGINT UNSIGNED NOT NULL,
    `schedule_id`           BIGINT UNSIGNED NOT NULL,
    `report_id`             BIGINT UNSIGNED NULL,
    `session_type_id`       BIGINT UNSIGNED NOT NULL,
    `status`                ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED','COMPLETED') NOT NULL DEFAULT 'PENDING',
    `booked_date`           DATE            NOT NULL,
    `booked_start_time`     TIME            NOT NULL,
    `booked_end_time`       TIME            NOT NULL,
    `cancellation_reason`   VARCHAR(500)    NULL,
    `cancelled_by`          ENUM('CLIENT','COUNSELOR','SYSTEM') NULL,
    `cancelled_at`          DATETIME        NULL,
    `created_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`            DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_booking_client` (`client_id`),
    INDEX `idx_booking_counselor` (`counselor_id`),
    INDEX `idx_booking_status` (`status`),
    CONSTRAINT `fk_booking_case` FOREIGN KEY (`case_id`) REFERENCES `counseling_cases`(`id`),
    CONSTRAINT `fk_booking_client` FOREIGN KEY (`client_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_booking_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_booking_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `counselor_schedules`(`id`),
    CONSTRAINT `fk_booking_report` FOREIGN KEY (`report_id`) REFERENCES `reports`(`id`),
    CONSTRAINT `fk_booking_type` FOREIGN KEY (`session_type_id`) REFERENCES `session_types`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 17. notifications — 알림
-- ============================================================
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT UNSIGNED NOT NULL,
    `type`            ENUM('BOOKING_REQ','CONFIRMED','CANCELLED','INTAKE_REQ','REMINDER','CHAT','REPORT','SYSTEM','COUNSELOR_APPROVED','COUNSELOR_REJECTED') NOT NULL,
    `title`           VARCHAR(200)    NOT NULL,
    `body`            TEXT            NULL,
    `reference_type`  VARCHAR(50)     NULL,
    `reference_id`    BIGINT UNSIGNED NULL,
    `is_read`         TINYINT(1)      NOT NULL DEFAULT 0,
    `sent_via`        ENUM('IN_APP','EMAIL','KAKAO') NOT NULL DEFAULT 'IN_APP',
    `sent_at`         DATETIME        NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_notification_user` (`user_id`),
    INDEX `idx_notification_read` (`user_id`, `is_read`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 18. audit_logs — 감사 로그
-- ============================================================
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `actor_id`      BIGINT UNSIGNED NOT NULL,
    `action`        VARCHAR(100)    NOT NULL,
    `target_table`  VARCHAR(100)    NOT NULL,
    `target_id`     BIGINT UNSIGNED NOT NULL,
    `ip_address`    VARCHAR(45)     NULL,
    `user_agent`    VARCHAR(500)    NULL,
    `metadata`      JSON            NULL,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_actor` (`actor_id`),
    INDEX `idx_audit_action` (`action`),
    CONSTRAINT `fk_audit_actor` FOREIGN KEY (`actor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
