package com.remitm.modules.payout.nsano.controller;

import com.remitm.modules.payout.nsano.service.NsanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public inbound callback endpoint for NSANO. Must be permitAll in SecurityConfig
 * (NSANO authenticates itself; we correlate by reference). Never throws — always 200.
 */
@RestController
@RequestMapping("/nsano")
@RequiredArgsConstructor
@Slf4j
public class NsanoCallbackController {

    private final NsanoService nsanoService;

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String msg) {
        try {
            nsanoService.handleCallback(reference, transactionId, status, msg);
        } catch (Exception ex) {
            log.error("NSANO callback: EXCEPTION | reference={}", reference, ex);
        }
        // Always acknowledge so NSANO does not retry indefinitely.
        return ResponseEntity.ok(Map.of("code", "00", "msg", "received"));
    }
}
