-- ============================================================
-- V10: 접수면접지 기능 추가
-- ============================================================

-- 1. users 테이블에 성별/생년월일 추가 (내담자 프로필 자동 조회용)
ALTER TABLE `users`
    ADD COLUMN `gender`     ENUM('MALE','FEMALE','OTHER') NULL AFTER `name`,
    ADD COLUMN `birth_date` DATE                          NULL AFTER `gender`;

-- 2. intake_forms 테이블 생성
CREATE TABLE IF NOT EXISTS `intake_forms` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `booking_id`   BIGINT UNSIGNED NOT NULL,
    `form_data`    JSON            NOT NULL COMMENT '접수면접지 JSON (name, gender, birthDate, counselorName 포함)',
    `submitted_at` DATETIME        NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_intake_booking` (`booking_id`),
    CONSTRAINT `fk_intake_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. notifications.type ENUM에 접수면접지 관련 타입 추가
ALTER TABLE `notifications` MODIFY COLUMN `type` ENUM(
    'BOOKING_REQ','CONFIRMED','CANCELLED','INTAKE_REQ',
    'INTAKE_FORM_REQUESTED','INTAKE_FORM_SUBMITTED',
    'REMINDER','CHAT','REPORT','SYSTEM',
    'COUNSELOR_APPROVED','COUNSELOR_REJECTED'
) NOT NULL;
