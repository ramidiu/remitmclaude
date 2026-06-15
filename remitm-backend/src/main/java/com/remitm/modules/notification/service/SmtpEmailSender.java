package com.remitm.modules.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Generic SMTP email sender (replaces the former Brevo HTTP API sender).
 * Backed by Spring's JavaMailSender, configured via spring.mail.* env vars
 * (MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD). Plug in any SMTP
 * provider (SES, SendGrid SMTP, Mailgun, a corporate relay, …) without code
 * changes. If no host/credentials are configured the send is skipped and the
 * caller's notification log records a failure — wire real SMTP creds to go live.
 */
@Service
@Slf4j
public class SmtpEmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.notification.email.from:noreply@remitm.com}")
    private String fromEmail;

    @Value("${spring.mail.host:}")
    private String mailHost;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean send(String toEmail, String subject, String htmlContent) {
        if (mailHost == null || mailHost.isBlank()) {
            log.warn("SMTP host not configured (spring.mail.host) — skipping email to {}", toEmail);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            mailSender.send(message);
            log.info("SMTP email sent to {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("SMTP email to {} failed: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
