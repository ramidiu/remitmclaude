package com.remitm.modules.notification.service;

import com.remitm.common.enums.NotificationChannel;
import com.remitm.common.enums.NotificationStatus;
import com.remitm.modules.notification.config.NotificationProperties;
import com.remitm.modules.notification.entity.NotificationLogEntity;
import com.remitm.modules.notification.repository.NotificationLogRepository;
import com.remitm.modules.user.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final ClickSendEmailSender clickSendEmailSender;
    private final NotificationProperties notificationProperties;
    private final NotificationLogRepository notificationLogRepository;
    // Code added by Naresh: System Controls Phase 5 — runtime master switch for outbound email.
    private final SystemConfigService systemConfigService;

    public boolean sendEmail(String to, String subject, String body) {
        return sendEmail(to, subject, body, null, null, null);
    }

    public boolean sendEmail(String to, String subject, String body,
                             Long userId, String templateCode, Long transactionId) {
        // Code added by Naresh: Read runtime control from system_config with safe fallback.
        // Default TRUE preserves existing behavior when the row is missing.
        if (!systemConfigService.getBoolean("notifications.email.enabled", true)) {
            log.info("Email skipped: notifications.email.enabled=false (to={}, template={})",
                    to, templateCode);
            return false;
        }

        int maxAttempts = notificationProperties.getRetry().getMaxAttempts();
        long delayMs = notificationProperties.getRetry().getDelayMs();

        NotificationLogEntity logEntry = NotificationLogEntity.builder()
                .userId(userId != null ? userId : 0L)
                .transactionId(transactionId)
                .templateCode(templateCode != null ? templateCode : "DIRECT")
                .channel(NotificationChannel.EMAIL)
                .recipient(to)
                .status(NotificationStatus.QUEUED)
                .retryCount(0)
                .build();
        logEntry = notificationLogRepository.save(logEntry);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            boolean sent = clickSendEmailSender.send(to, subject, body);
            if (sent) {
                logEntry.setStatus(NotificationStatus.SENT);
                logEntry.setSentAt(LocalDateTime.now());
                logEntry.setRetryCount(attempt - 1);
                notificationLogRepository.save(logEntry);
                log.info("Email sent successfully to: {} (attempt {})", to, attempt);
                return true;
            }
            log.warn("Failed to send email to: {} (attempt {}/{})", to, attempt, maxAttempts);
            logEntry.setRetryCount(attempt);
            if (attempt < maxAttempts) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); break;
                }
            }
        }

        logEntry.setStatus(NotificationStatus.FAILED);
        notificationLogRepository.save(logEntry);
        log.error("Failed to send email to: {} after {} attempts", to, maxAttempts);
        return false;
    }
}
