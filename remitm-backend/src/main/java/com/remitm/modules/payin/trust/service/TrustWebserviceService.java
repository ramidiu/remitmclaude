package com.remitm.modules.payin.trust.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-side verification of a Trust Payments (SecureTrading) transaction via the
 * webservices JSON API — a TRANSACTIONQUERY. This lets the backend independently
 * confirm the real payment outcome (errorcode / settlestatus) instead of trusting
 * the browser redirect params, which a user could spoof.
 *
 * The original RemitM app declared this endpoint but never wired the call; here it
 * is implemented properly but gated behind {@code trust.webservices.verify-enabled}
 * (default false) because it needs real Trust webservices credentials (a webservices
 * "alias" username + password issued by Trust for the site reference). When disabled
 * or unconfigured, callers fall back to the redirect params (existing behaviour).
 */
@Service
@Slf4j
public class TrustWebserviceService {

    @Value("${trust.webservices.url:https://webservices.securetrading.net/json/}")
    private String url;

    @Value("${trust.webservices.verify-enabled:false}")
    private boolean verifyEnabled;

    @Value("${trust.webservices.alias:}")
    private String alias;          // webservices username/alias issued by Trust

    @Value("${trust.webservices.password:}")
    private String password;       // webservices password

    @Value("${trust.webservices.sitereference:test_remitm107329}")
    private String siteReference;

    private final RestTemplate restTemplate = new RestTemplate();
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public boolean isEnabled() {
        return verifyEnabled && alias != null && !alias.isBlank()
                && password != null && !password.isBlank();
    }

    /**
     * Runs a TRANSACTIONQUERY for the given Trust transaction reference and returns the
     * server-side errorcode ("0" == success). Returns empty if verification is disabled,
     * unconfigured, or the call fails — callers should then fall back to redirect params.
     */
    public Optional<String> queryErrorCode(String transactionReference) {
        if (!isEnabled() || transactionReference == null || transactionReference.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> filter = Map.of(
                    "sitereference", List.of(Map.of("value", siteReference)),
                    "transactionreference", List.of(Map.of("value", transactionReference))
            );
            Map<String, Object> request = Map.of(
                    "requesttypedescriptions", List.of("TRANSACTIONQUERY"),
                    "filter", filter
            );
            Map<String, Object> body = Map.of(
                    "alias", alias,
                    "version", "1.00",
                    "request", List.of(request)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(alias, password);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                log.warn("Trust TRANSACTIONQUERY for {} returned status {}", transactionReference, resp.getStatusCode());
                return Optional.empty();
            }
            JsonNode root = mapper.readTree(resp.getBody());
            // response[0].records[0].errorcode  (records hold the matched transaction)
            JsonNode response0 = root.path("response").path(0);
            JsonNode record0 = response0.path("records").path(0);
            String errorcode = record0.path("errorcode").asText(null);
            if (errorcode == null) {
                // Fall back to the response-level errorcode (query itself failed → not a payment result)
                errorcode = response0.path("errorcode").asText(null);
                log.warn("Trust TRANSACTIONQUERY for {}: no record errorcode (query errorcode={})",
                        transactionReference, errorcode);
                return Optional.empty();
            }
            log.info("Trust TRANSACTIONQUERY verified {} -> errorcode={}", transactionReference, errorcode);
            return Optional.of(errorcode);
        } catch (Exception e) {
            log.error("Trust TRANSACTIONQUERY for {} failed: {}", transactionReference, e.getMessage());
            return Optional.empty();
        }
    }
}
