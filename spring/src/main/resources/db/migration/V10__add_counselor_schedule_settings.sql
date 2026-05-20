SET NAMES utf8mb4;
SET time_zone = '+09:00';

-- ============================================================
-- counselor_schedule_settings — 상담사 슬롯 단위 설정
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_schedule_settings` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`  BIGINT UNSIGNED NOT NULL,
    `slot_unit`     INT             NOT NULL DEFAULT 60 COMMENT '슬롯 단위(분), 최초 1회 설정 후 변경 불가',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_css_counselor` (`counselor_id`),
    CONSTRAINT `fk_css_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- counselor_dayoffs — 브레이크타임 / 휴무일
-- ============================================================
CREATE TABLE IF NOT EXISTS `counselor_dayoffs` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`  BIGINT UNSIGNED NOT NULL,
    `dayoff_type`   ENUM('BREAKTIME','REGULAR_HOLIDAY','TEMPORARY_HOLIDAY') NOT NULL,
    `day_of_week`   ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NULL
                    COMMENT '정기 휴무일에 사용',
    `dayoff_date`   DATE            NULL COMMENT '임시 휴무/브레이크타임 날짜',
    `start_time`    TIME            NULL COMMENT '브레이크타임 시작',
    `end_time`      TIME            NULL COMMENT '브레이크타임 종료',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_dayoff_counselor_type` (`counselor_id`, `dayoff_type`),
    INDEX `idx_dayoff_counselor_date` (`counselor_id`, `dayoff_date`),
    CONSTRAINT `fk_dayoff_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
