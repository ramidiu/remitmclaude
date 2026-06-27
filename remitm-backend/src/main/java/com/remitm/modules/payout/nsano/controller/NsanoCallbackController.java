package com.remitm.modules.payout.nsano.controller;

import com.remitm.modules.payout.nsano.config.NsanoProperties;
import com.remitm.modules.payout.nsano.service.NsanoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public inbound callback endpoint for NSANO. Must be permitAll in SecurityConfig
 * (NSANO authenticates itself; we correlate by reference). Never throws — always 200.
 *
 * <p>Access is restricted to NSANO's callback source IPs ({@code nsano.callback-allowed-ips}).
 * The endpoint is reached through host nginx, which forwards the real client IP in
 * {@code X-Forwarded-For} / {@code X-Real-IP}; requests from any other IP are rejected with 403.
 */
@RestController
@RequestMapping("/nsano")
@RequiredArgsConstructor
@Slf4j
public class NsanoCallbackController {

    private final NsanoService nsanoService;
    private final NsanoProperties nsanoProperties;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            HttpServletRequest request,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String msg) {

        String clientIp = resolveClientIp(request);
        if (!isAllowed(clientIp)) {
            log.warn("NSANO callback: REJECTED from non-whitelisted IP={} | reference={}", clientIp, reference);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("code", "99", "msg", "forbidden"));
        }

        try {
            nsanoService.handleCallback(reference, transactionId, status, msg);
        } catch (Exception ex) {
            log.error("NSANO callback: EXCEPTION | reference={}", reference, ex);
        }
        // Always acknowledge so NSANO does not retry indefinitely.
        return ResponseEntity.ok(Map.of("code", "00", "msg", "received"));
    }

    /** True if the IP check is disabled (no IPs configured) or {@code clientIp} is whitelisted. */
    private boolean isAllowed(String clientIp) {
        var allowed = nsanoProperties.getCallbackAllowedIps();
        if (allowed == null || allowed.isEmpty()) {
            return true; // fail-open when unconfigured (local/staging)
        }
        return clientIp != null && allowed.contains(clientIp);
    }

    /**
     * Resolve the originating client IP. Behind nginx the real IP is the left-most entry of
     * {@code X-Forwarded-For} (then {@code X-Real-IP}); falls back to the socket address for
     * direct calls. The backend listens only on localhost, so these headers are proxy-supplied.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
