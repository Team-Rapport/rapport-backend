-- bookings.concern 컬럼 추가 (V2__add_favorites_and_booking_concern.sql 스킵된 경우 보정)
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bookings' AND COLUMN_NAME = 'concern');
SET @q = IF(@s = 0,
    'ALTER TABLE `bookings` ADD COLUMN `concern` TEXT NULL COMMENT ''내담자 주요 고민'' AFTER `session_type_id`',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- favorites 테이블 생성 (V2 스킵된 경우 보정)
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

-- chat_rooms: V3 스키마(is_active, last_message_at)가 적용된 경우 V4 스키마로 교정
-- CREATE TABLE IF NOT EXISTS 로 V4가 skip 된 환경을 위한 보정 마이그레이션

-- booking_id 컬럼 추가 (없을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND COLUMN_NAME = 'booking_id');
SET @q = IF(@s = 0,
    'ALTER TABLE `chat_rooms` ADD COLUMN `booking_id` BIGINT UNSIGNED NULL AFTER `id`',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- status 컬럼 추가 (없을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND COLUMN_NAME = 'status');
SET @q = IF(@s = 0,
    'ALTER TABLE `chat_rooms` ADD COLUMN `status` ENUM(''OPEN'',''CLOSED'') NOT NULL DEFAULT ''OPEN''',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- is_active 컬럼 제거 (있을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND COLUMN_NAME = 'is_active');
SET @q = IF(@s > 0,
    'ALTER TABLE `chat_rooms` DROP COLUMN `is_active`',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- last_message_at 컬럼 제거 (있을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND COLUMN_NAME = 'last_message_at');
SET @q = IF(@s > 0,
    'ALTER TABLE `chat_rooms` DROP COLUMN `last_message_at`',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- uk_chat_room_booking unique key 추가 (없을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.STATISTICS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND INDEX_NAME = 'uk_chat_room_booking');
SET @q = IF(@s = 0,
    'ALTER TABLE `chat_rooms` ADD UNIQUE KEY `uk_chat_room_booking` (`booking_id`)',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- fk_chat_room_booking FK 추가 (없을 때만)
SET @s = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'chat_rooms' AND CONSTRAINT_NAME = 'fk_chat_room_booking');
SET @q = IF(@s = 0,
    'ALTER TABLE `chat_rooms` ADD CONSTRAINT `fk_chat_room_booking` FOREIGN KEY (`booking_id`) REFERENCES `bookings`(`id`)',
    'SELECT 1');
PREPARE stmt FROM @q; EXECUTE stmt; DEALLOCATE PREPARE stmt;
