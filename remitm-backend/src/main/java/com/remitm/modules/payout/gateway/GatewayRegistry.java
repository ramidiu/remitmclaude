package com.remitm.modules.payout.gateway;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Discovers every PayoutGateway bean and looks them up by type. New gateways auto-register. */
@Component
public class GatewayRegistry {

    private final Map<String, PayoutGateway> byType;

    public GatewayRegistry(List<PayoutGateway> gateways) {
        this.byType = gateways.stream()
                .collect(Collectors.toMap(g -> g.getType().toUpperCase(), g -> g));
    }

    public Optional<PayoutGateway> get(String type) {
        return type == null ? Optional.empty() : Optional.ofNullable(byType.get(type.toUpperCase()));
    }

    /** Always resolves to a gateway — falls back to MANUAL when the type is unknown/unconfigured. */
    public PayoutGateway getOrManual(String type) {
        return get(type).orElseGet(() -> byType.get("MANUAL"));
    }

    /** All registered gateway type keys (for admin dropdowns). */
    public java.util.Set<String> types() {
        return new java.util.TreeSet<>(byType.keySet());
    }
}
