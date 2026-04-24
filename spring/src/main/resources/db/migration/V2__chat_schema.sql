-- ============================================================
-- Rapport DB Schema v2.1
-- Flyway Migration V2__chat_schema.sql
-- 내담자-상담사 실시간 채팅 (STOMP WebSocket)
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET time_zone = '+09:00';

-- ============================================================
-- 1. chat_rooms — 채팅방 (booking 1:1 또는 독립 생성)
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_rooms` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `booking_id`    BIGINT UNSIGNED NULL     COMMENT '예약 확정 시 자동 생성되는 경우 연결',
    `client_id`     BIGINT UNSIGNED NOT NULL,
    `counselor_id`  BIGINT UNSIGNED NOT NULL,
    `status`        ENUM('OPEN','CLOSED')    NOT NULL DEFAULT 'OPEN',
    `created_at`    DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME                 NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_room_booking` (`booking_id`),
    INDEX `idx_chat_room_client`   (`client_id`),
    INDEX `idx_chat_room_counselor`(`counselor_id`),
    CONSTRAINT `fk_chat_room_booking`   FOREIGN KEY (`booking_id`)   REFERENCES `bookings`(`id`),
    CONSTRAINT `fk_chat_room_client`    FOREIGN KEY (`client_id`)    REFERENCES `users`(`id`),
    CONSTRAINT `fk_chat_room_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 2. chat_messages — 채팅 메시지
-- ============================================================
CREATE TABLE IF NOT EXISTS `chat_messages` (
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id`       BIGINT UNSIGNED NOT NULL,
    `sender_id`     BIGINT UNSIGNED NOT NULL,
    `content`       TEXT            NOT NULL,
    `message_type`  ENUM('TEXT','IMAGE','SYSTEM') NOT NULL DEFAULT 'TEXT',
    `is_read`       TINYINT(1)      NOT NULL DEFAULT 0,
    `sent_at`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_chat_message_room`  (`room_id`, `sent_at`),
    INDEX `idx_chat_message_sender`(`sender_id`),
    CONSTRAINT `fk_chat_message_room`   FOREIGN KEY (`room_id`)   REFERENCES `chat_rooms`(`id`),
    CONSTRAINT `fk_chat_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
