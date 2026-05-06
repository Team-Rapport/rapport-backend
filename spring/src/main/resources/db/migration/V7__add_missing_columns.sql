-- V2__add_favorites_and_booking_concern.sql 이 스킵된 환경 보정

-- bookings.concern
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'concern');
SET @q = IF(@s = 0,
    'ALTER TABLE `bookings` ADD COLUMN `concern` TEXT NULL COMMENT ''내담자 주요 고민'' AFTER `session_type_id`',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- favorites 테이블
CREATE TABLE IF NOT EXISTS `favorites` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `client_id`    BIGINT UNSIGNED NOT NULL,
    `counselor_id` BIGINT UNSIGNED NOT NULL,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite` (`client_id`, `counselor_id`),
    INDEX `idx_favorite_client` (`client_id`),
    CONSTRAINT `fk_favorite_client`    FOREIGN KEY (`client_id`)   REFERENCES `users`(`id`),
    CONSTRAINT `fk_favorite_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
