-- chat_rooms 테이블
CREATE TABLE IF NOT EXISTS `chat_rooms` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `client_id`       BIGINT UNSIGNED NOT NULL,
    `counselor_id`    BIGINT UNSIGNED NOT NULL,
    `is_active`       TINYINT(1)      NOT NULL DEFAULT 1,
    `last_message_at` DATETIME        NULL,
    `created_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_chatroom_client` (`client_id`),
    INDEX `idx_chatroom_counselor` (`counselor_id`),
    CONSTRAINT `fk_chatroom_client`   FOREIGN KEY (`client_id`)   REFERENCES `users`(`id`),
    CONSTRAINT `fk_chatroom_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- direct_messages 테이블
CREATE TABLE IF NOT EXISTS `direct_messages` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `room_id`      BIGINT UNSIGNED NOT NULL,
    `sender_id`    BIGINT UNSIGNED NOT NULL,
    `content`      TEXT            NOT NULL,
    `message_type` ENUM('TEXT','IMAGE','FILE') NOT NULL DEFAULT 'TEXT',
    `file_url`     VARCHAR(500)    NULL,
    `is_read`      TINYINT(1)      NOT NULL DEFAULT 0,
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_dm_room` (`room_id`),
    CONSTRAINT `fk_dm_room`   FOREIGN KEY (`room_id`)   REFERENCES `chat_rooms`(`id`),
    CONSTRAINT `fk_dm_sender` FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- session_notes 테이블
CREATE TABLE IF NOT EXISTS `session_notes` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `counselor_id`      BIGINT UNSIGNED NOT NULL,
    `session_id`        BIGINT UNSIGNED NULL,
    `note_type`         ENUM('SOAP','PROGRESS','GENERAL') NOT NULL DEFAULT 'GENERAL',
    `content_encrypted` TEXT            NOT NULL,
    `is_confidential`   TINYINT(1)      NOT NULL DEFAULT 1,
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at`        DATETIME        NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_note_counselor` (`counselor_id`),
    CONSTRAINT `fk_note_counselor` FOREIGN KEY (`counselor_id`) REFERENCES `users`(`id`),
    CONSTRAINT `fk_note_session`   FOREIGN KEY (`session_id`)   REFERENCES `bookings`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;