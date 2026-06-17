-- ============================================================
-- V4__add_email_verification.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `email_verifications` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `email`       VARCHAR(255)    NOT NULL,
    `code`        VARCHAR(10)     NOT NULL,
    `is_verified` TINYINT(1)      NOT NULL DEFAULT 0,
    `expires_at`  DATETIME        NOT NULL,
    `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_ev_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
