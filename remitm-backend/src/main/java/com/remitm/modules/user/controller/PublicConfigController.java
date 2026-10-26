package com.remitm.modules.user.controller;

import com.remitm.modules.user.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public, unauthenticated app configuration surfaced to the customer app so the UI reflects the
 * operator's System Controls (system_config) instead of hardcoded values / always-on features.
 * The client loads this once at startup and hides/disables features and drives timers accordingly.
 *
 * Single source of truth for customer-facing flags — mirrors the existing /api/payin/creation-flags
 * pattern but generalised. Only NON-sensitive, customer-relevant settings are exposed here.
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Public Config", description = "Public customer-app configuration flags")
public class PublicConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/app-config")
    @Operation(summary = "Customer app config",
            description = "Feature toggles + timing values so the customer UI matches the admin's System Controls")
    public ResponseEntity<Map<String, Object>> appConfig() {
        Map<String, Object> cfg = new LinkedHashMap<>();
        // Feature toggles (operations)
        cfg.put("registrationEnabled", systemConfigService.getBoolean("registration.enabled", true));
        cfg.put("beneficiaryEnabled", systemConfigService.getBoolean("beneficiary.enabled", true));
        cfg.put("transactionsEnabled", systemConfigService.getBoolean("transactions.enabled", true));
        cfg.put("payinEnabled", systemConfigService.getBoolean("payin.enabled", true));
        cfg.put("payoutEnabled", systemConfigService.getBoolean("payout.enabled", true));
        cfg.put("liveRatesEnabled", systemConfigService.getBoolean("home.live_rates_enabled", true));
        // Maintenance (so the client can show a banner proactively, not only on a 503)
        cfg.put("maintenanceMode", systemConfigService.getBoolean("maintenance.mode.enabled", false));
        cfg.put("maintenanceMessage", systemConfigService.getString("maintenance.message", ""));
        // Timing values
        int otpTtlMinutes = systemConfigService.getInt("OTP_TTL_MINUTES", 10);
        cfg.put("otpTtlMinutes", otpTtlMinutes);
        cfg.put("otpTtlSeconds", otpTtlMinutes * 60);
        return ResponseEntity.ok(cfg);
    }
}
