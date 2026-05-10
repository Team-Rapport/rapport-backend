-- bookings.schedule_id: NOT NULL → NULL (MVP 단계에서 스케줄 없이 예약 생성 허용)
ALTER TABLE `bookings` MODIFY COLUMN `schedule_id` BIGINT UNSIGNED NULL;
