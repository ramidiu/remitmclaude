package com.remitm.modules.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickSend email sender — ported faithfully from the RemitM production app
 * (com.kws.remitm.helper.SendMail). Sends HTML email via the ClickSend REST API.
 *
 * POST https://rest.clicksend.com/v3/email/send
 * Basic auth: clicksend.username / clicksend.password
 * Body: { subject, body, from:{email_address_id, name}, to:[{email, name}] }
 *
 * Same send(to, subject, html) interface as the other senders so it is a drop-in
 * replacement. Configure via clicksend.* env vars (defaults are the RemitM values).
 */
@Service
@Slf4j
public class ClickSendEmailSender {

    @Value("${clicksend.url:https://rest.clicksend.com/v3/email/send}")
    private String url;

    @Value("${clicksend.username:}")
    private String username;

    @Value("${clicksend.password:}")
    private String password;

    @Value("${clicksend.from.email-address-id:25219}")
    private Integer fromEmailAddressId;

    @Value("${clicksend.from.name:Remitm}")
    private String fromName;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean send(String toEmail, String subject, String htmlContent) {
        return send(toEmail, subject, htmlContent, toEmail);
    }

    public boolean send(String toEmail, String subject, String htmlContent, String toName) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            log.warn("ClickSend credentials not configured (clicksend.username/password) — skipping email to {}", toEmail);
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("subject", subject);
            body.put("body", htmlContent);

            Map<String, Object> from = new HashMap<>();
            from.put("email_address_id", fromEmailAddressId);
            from.put("name", fromName);
            body.put("from", from);

            Map<String, Object> to = new HashMap<>();
            to.put("email", toEmail);
            to.put("name", toName != null && !toName.isBlank() ? toName : toEmail);
            List<Object> toList = new ArrayList<>();
            toList.add(to);
            body.put("to", toList);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(username, password);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("ClickSend email sent to {}", toEmail);
                return true;
            }
            log.warn("ClickSend email to {} failed with status {}", toEmail, response.getStatusCode());
        } catch (Exception e) {
            log.error("ClickSend email to {} error: {}", toEmail, e.getMessage());
        }
        return false;
    }
}
