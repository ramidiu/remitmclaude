package com.remitm.modules.payout.gateway.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remitm.modules.payout.gateway.*;
import com.remitm.modules.payout.zeepay.dto.ZeepayInitiateResponse;
import com.remitm.modules.payout.zeepay.service.ZeepayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** Zeepay (Ghana/Zimbabwe/Zambia/…) gateway — wraps ZeepayService and normalizes its response. */
@Component
@RequiredArgsConstructor
public class ZeepayGateway implements PayoutGateway {

    private final ZeepayService zeepayService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override public String getType() { return "ZEEPAY"; }

    @Override public GatewayCapabilities getCapabilities() {
        return new GatewayCapabilities(true, true);
    }

    @Override public ValidationResult validateRecipient(ValidationRequest req) {
        String raw;
        if ("BANK_DEPOSIT".equalsIgnoreCase(req.getDeliveryMethod())) {
            // Zeepay bank validation uses the routing number (fall back to bankOrProvider if absent).
            String routing = req.getRoutingNumber() != null && !req.getRoutingNumber().isBlank()
                    ? req.getRoutingNumber() : req.getBankOrProvider();
            raw = zeepayService.validateRecipient("Bank", null, null,
                    routing, req.getAccountNumber(), req.getReceivingCountry());
        } else {
            raw = zeepayService.validateRecipient("Wallet", req.getBankOrProvider(), req.getAccountNumber(),
                    null, null, req.getReceivingCountry());
        }
        return ValidationResult.of(extractName(raw));
    }

    @SuppressWarnings("unchecked")
    private String extractName(String raw) {
        if (raw == null) return null;
        try {
            Map<String, Object> m = mapper.readValue(raw, Map.class);
            Object data = m.get("data");
            Map<String, Object> d = (data instanceof Map) ? (Map<String, Object>) data : m;
            for (String k : new String[]{"account_name", "accountName", "name", "customer_name", "fullName"}) {
                Object v = d.get(k);
                if (v != null && !v.toString().isBlank()) return v.toString();
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Override public Map<String, Object> disburse(String referenceNumber) {
        ZeepayInitiateResponse r = zeepayService.disburse(referenceNumber);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("gateway", "ZEEPAY");
        m.put("code", r != null ? r.getCode() : null);
        m.put("message", r != null ? r.getMessage() : null);
        m.put("success", r != null && "411".equals(r.getCode()));
        return m;
    }
}
