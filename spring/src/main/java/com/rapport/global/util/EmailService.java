package com.rapport.global.util;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ===== 상담사 승인 이메일 =====

    @Async
    public void sendCounselorApprovalEmail(String toEmail, String counselorName) {
        Context ctx = new Context();
        ctx.setVariable("counselorName", counselorName);
        ctx.setVariable("loginUrl", frontendUrl + "/login");
        ctx.setVariable("profileUrl", frontendUrl + "/counselor/profile");

        String subject = "[라포] 상담사 심사 승인 안내";
        String html = templateEngine.process("email/counselor-approved", ctx);
        sendHtmlEmail(toEmail, subject, html);
        log.info("Approval email sent to {}", toEmail);
    }

    // ===== 상담사 반려 이메일 =====

    @Async
    public void sendCounselorRejectionEmail(String toEmail, String counselorName, String reason) {
        Context ctx = new Context();
        ctx.setVariable("counselorName", counselorName);
        ctx.setVariable("reason", reason);
        ctx.setVariable("reapplyUrl", frontendUrl + "/signup/counselor");

        String subject = "[라포] 상담사 심사 반려 안내";
        String html = templateEngine.process("email/counselor-rejected", ctx);
        sendHtmlEmail(toEmail, subject, html);
        log.info("Rejection email sent to {}", toEmail);
    }

    // ===== 예약 확정 이메일 =====

    @Async
    public void sendBookingConfirmedEmail(String toEmail, String userName,
                                          String counselorName, String dateTime) {
        Context ctx = new Context();
        ctx.setVariable("userName", userName);
        ctx.setVariable("counselorName", counselorName);
        ctx.setVariable("dateTime", dateTime);
        ctx.setVariable("bookingsUrl", frontendUrl + "/my/bookings");

        String subject = "[라포] 상담 예약이 확정되었습니다.";
        String html = templateEngine.process("email/booking-confirmed", ctx);
        sendHtmlEmail(toEmail, subject, html);
    }

    // ===== 공통 HTML 이메일 발송 메서드 =====

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // 이메일 발송 실패는 비즈니스 로직을 중단시키지 않음 (비동기 처리)
        }
    }
}
