package com.remitm.modules.payin.fire.service;

import com.remitm.modules.payin.fire.config.FireProperties;
import com.remitm.modules.transaction.entity.TransactionEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client for the Fire (fire.com) Open Banking pay-in API.
 *
 * <p>The flow is:
 * <ol>
 *   <li>{@link #getAccessToken()} — exchange the refresh token + a per-request
 *       nonce/clientSecret for a short-lived bearer access token.</li>
 *   <li>{@link #createPaymentRequest(String, TransactionEntity, String)} —
 *       create a hosted payment request; the returned {@code code} builds the
 *       URL the payer is redirected to.</li>
 *   <li>{@link #listAspsps(String)} — list the supported banks (ASPSPs).</li>
 * </ol>
 *
 * <p>This service is stateless and thread-safe: the access token is never
 * stored on the instance or in the HTTP session — callers pass it explicitly.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FireService {

    private static final DateTimeFormatter NONCE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final FireProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Builds the 12-digit nonce: current time as {@code yyyy/MM/dd HH:mm:ss},
     * drop the first two chars (the century of the 4-digit year), then strip
     * all {@code /}, {@code :} and spaces. e.g. {@code 260611143005}.
     */
    String generateNonce() {
        String formatted = LocalDateTime.now().format(NONCE_FORMAT); // e.g. 2026/06/11 14:30:05
        String withoutCentury = formatted.substring(2);              // 26/06/11 14:30:05
        return withoutCentury.replace("/", "").replace(":", "").replace(" ", ""); // 260611143005
    }

    /**
     * clientSecret = lowercase hex of SHA-256(nonce + clientKey), padded to 64 chars.
     */
    String generateClientSecret(String nonce) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((nonce + props.getClientKey()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            String result = hex.toString();
            while (result.length() < 64) {
                result = "0" + result;
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on a standard JVM.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Exchanges the refresh token for a short-lived bearer access token.
     *
     * @return the access token, or {@code null} if the call failed.
     */
    @SuppressWarnings("unchecked")
    public String getAccessToken() {
        try {
            String nonce = generateNonce();
            String clientSecret = generateClientSecret(nonce);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("clientId", props.getClientId());
            body.put("refreshToken", props.getRefreshToken());
            body.put("nonce", nonce);
            body.put("grantType", "AccessToken");
            body.put("clientSecret", clientSecret);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    props.getTokenUrl(), request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("Fire access token response had no body");
                return null;
            }
            Object accessToken = responseBody.get("accessToken");
            if (accessToken == null) {
                log.error("Fire access token response missing accessToken: {}", responseBody);
                return null;
            }
            log.info("Fire access token obtained (businessId={})", responseBody.get("businessId"));
            return accessToken.toString();
        } catch (Exception e) {
            log.error("Failed to obtain Fire access token: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Creates a Fire hosted payment request for the given transaction.
     *
     * @param accessToken the bearer access token from {@link #getAccessToken()}
     * @param tx          our transaction
     * @param description description shown on the hosted payment page
     * @return the payment-request {@code code} used to build the redirect URL,
     *         or {@code null} on failure.
     */
    @SuppressWarnings("unchecked")
    public String createPaymentRequest(String accessToken, TransactionEntity tx, String description) {
        try {
            BigDecimal sendAmount = tx.getSendAmount() != null ? tx.getSendAmount() : BigDecimal.ZERO;
            // Minor units (pence): scale by 100, drop any fractional remainder safely.
            String minorUnits = sendAmount.movePointRight(2).toBigInteger().toString();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("type", "OTHER");
            body.put("icanTo", parseIcanTo(props.getIcanTo()));
            body.put("currency", "GBP");
            body.put("amount", minorUnits);
            body.put("myRef", tx.getReferenceNumber());
            body.put("description", description);
            body.put("maxNumberPayments", 1);
            body.put("returnUrl", props.getReturnUrl());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    props.getApiBase() + "/business/v1/paymentrequests", request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("code") == null) {
                log.error("Fire payment request response missing code: {}", responseBody);
                return null;
            }
            String code = responseBody.get("code").toString();
            log.info("Fire payment request created for txn {} -> code {}", tx.getReferenceNumber(), code);
            return code;
        } catch (Exception e) {
            log.error("Failed to create Fire payment request for txn {}: {}",
                    tx.getReferenceNumber(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Lists the supported ASPSPs (banks) for the given currency.
     *
     * @param accessToken the bearer access token from {@link #getAccessToken()}
     * @return the raw Fire response ({@code {total, aspsps:[...]}}), or {@code null} on failure.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> listAspsps(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    props.getApiBase() + "/business/v1/aspsps?currency=GBP",
                    org.springframework.http.HttpMethod.GET, request, Map.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to list Fire ASPSPs: {}", e.getMessage(), e);
            return null;
        }
    }

    public String buildHostedUrl(String code) {
        return props.getPaymentsBase() + "/" + code;
    }

    public FireProperties getProps() {
        return props;
    }

    /** icanTo is numeric on the Fire side; send as a number when possible, else the raw string. */
    private Object parseIcanTo(String icanTo) {
        if (icanTo == null) {
            return null;
        }
        try {
            return Long.parseLong(icanTo.trim());
        } catch (NumberFormatException e) {
            return icanTo;
        }
    }
}
