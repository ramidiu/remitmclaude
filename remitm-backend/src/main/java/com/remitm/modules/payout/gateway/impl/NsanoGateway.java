package com.remitm.modules.payout.gateway.impl;

import com.remitm.modules.payout.gateway.*;
import com.remitm.modules.payout.nsano.dto.NsanoApiResponse;
import com.remitm.modules.payout.nsano.service.NsanoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nsano (Ghana) gateway — wraps the existing NsanoService behind the common interface. */
@Component
@RequiredArgsConstructor
public class NsanoGateway implements PayoutGateway {

    private final NsanoService nsanoService;

    @Override public String getType() { return "NSANO"; }

    @Override public GatewayCapabilities getCapabilities() {
        return new GatewayCapabilities(true, true); // live name-check, async payout
    }

    @Override public ValidationResult validateRecipient(ValidationRequest req) {
        boolean wallet = !"BANK_DEPOSIT".equalsIgnoreCase(req.getDeliveryMethod());
        String name = nsanoService.nameCheck(wallet, req.getAccountNumber(), req.getBankOrProvider());
        return ValidationResult.of(name);
    }

    @Override public Map<String, Object> disburse(String referenceNumber) {
        NsanoApiResponse r = nsanoService.disburse(referenceNumber);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gateway", "NSANO");
        m.put("code", r != null ? r.getCode() : null);
        m.put("message", r != null ? r.getMsg() : null);
        m.put("success", r != null && "00".equals(r.getCode()));
        return m;
    }
}
