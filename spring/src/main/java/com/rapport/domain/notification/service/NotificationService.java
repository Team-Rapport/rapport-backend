package com.rapport.domain.notification.service;

import com.rapport.domain.notification.entity.Notification;
import com.rapport.domain.notification.entity.NotificationRepository;
import com.rapport.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async("emailExecutor")
    @Transactional
    public void send(User user, Notification.NotificationType type,
                     String title, String body,
                     String referenceType, Long referenceId) {
        Notification notification = Notification.create(
                user, type, title, body, referenceType, referenceId);
        notificationRepository.save(notification);
        log.info("Notification saved: userId={}, type={}", user.getId(), type);
    }

    public void notifyBookingReceived(User counselor, Long bookingId, String clientName) {
        send(counselor, Notification.NotificationType.BOOKING_REQ,
                "새 예약 요청",
                clientName + " 님이 예약을 요청했습니다.",
                "BOOKING", bookingId);
    }

    public void notifyBookingConfirmed(User client, Long bookingId, String counselorName) {
        send(client, Notification.NotificationType.CONFIRMED,
                "예약이 확정되었습니다",
                counselorName + " 상담사님이 예약을 수락했습니다.",
                "BOOKING", bookingId);
    }

    public void notifyBookingRejected(User client, Long bookingId, String counselorName) {
        send(client, Notification.NotificationType.CANCELLED,
                "예약이 거절되었습니다",
                counselorName + " 상담사님이 예약을 거절했습니다.",
                "BOOKING", bookingId);
    }

    public void notifyBookingCancelled(User counselor, Long bookingId, String clientName) {
        send(counselor, Notification.NotificationType.CANCELLED,
                "예약이 취소되었습니다",
                clientName + " 님이 예약을 취소했습니다.",
                "BOOKING", bookingId);
    }
}
