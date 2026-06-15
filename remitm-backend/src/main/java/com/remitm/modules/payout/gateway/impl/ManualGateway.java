package com.remitm.modules.payout.gateway.impl;

import com.remitm.modules.payout.gateway.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * MANUAL gateway (null-object) — for corridors handled by a human/partner with no live API.
 * No name-check; "disburse" is a no-op the payout partner completes from their portal.
 */
@Component
public class ManualGateway implements PayoutGateway {

    @Override public String getType() { return "MANUAL"; }

    @Override public GatewayCapabilities getCapabilities() {
        return new GatewayCapabilities(false, false);
    }

    @Override public ValidationResult validateRecipient(ValidationRequest req) {
        return ValidationResult.notFound();
    }

    @Override public Map<String, Object> disburse(String referenceNumber) {
        return Map.of("gateway", "MANUAL", "success", false,
                "message", "Manual payout — completed by the payout partner from their portal");
    }
}
