-- V8에서 실수로 삭제된 direct_messages 테이블 복구
-- directchat 모듈(ChatControllers)에서 실제 메시지 저장·조회에 사용
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
    INDEX `idx_dm_room`    (`room_id`),
    INDEX `idx_dm_sender`  (`sender_id`),
    CONSTRAINT `fk_dm_room`   FOREIGN KEY (`room_id`)   REFERENCES `chat_rooms`(`id`),
    CONSTRAINT `fk_dm_sender` FOREIGN KEY (`sender_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
