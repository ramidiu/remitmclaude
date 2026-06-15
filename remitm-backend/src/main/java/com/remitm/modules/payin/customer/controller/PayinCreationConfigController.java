package com.remitm.modules.payin.customer.controller;

import com.remitm.modules.user.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Read-only feature flags that tell the pay-in partner portal whether the "Create Customer" /
 * "Create Transaction" options should be shown. The super-admin owns these via System Controls
 * (keys payin.customer_creation.enabled / payin.transaction_creation.enabled).
 */
@RestController
@RequestMapping("/api/payin/creation-flags")
@RequiredArgsConstructor
public class PayinCreationConfigController {

    public static final String CUSTOMER_KEY = "payin.customer_creation.enabled";
    public static final String TRANSACTION_KEY = "payin.transaction_creation.enabled";

    private final SystemConfigService systemConfigService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PAYIN_PARTNER', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> flags() {
        return ResponseEntity.ok(Map.of(
                "customerCreation", systemConfigService.getBoolean(CUSTOMER_KEY, true),
                "transactionCreation", systemConfigService.getBoolean(TRANSACTION_KEY, true)));
    }
}
