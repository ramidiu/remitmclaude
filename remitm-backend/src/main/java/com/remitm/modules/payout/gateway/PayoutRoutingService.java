package com.remitm.modules.payout.gateway;

import com.remitm.common.enums.DeliveryMethod;
import com.remitm.modules.fx.entity.CorridorDeliveryMethodEntity;
import com.remitm.modules.fx.entity.CorridorEntity;
import com.remitm.modules.fx.repository.CorridorDeliveryMethodRepository;
import com.remitm.modules.fx.repository.CorridorRepository;
import com.remitm.modules.transaction.entity.PayoutPartner;
import com.remitm.modules.transaction.repository.PayoutPartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Single source of truth for "which gateway handles this corridor + delivery method".
 * Resolves: receiveCurrency + deliveryMethod → active corridor_delivery_methods row →
 * payout_partner → partner.gateway. Falls back to MANUAL when nothing is configured.
 * No hardcoded country/provider logic — it's all driven by corridor_delivery_methods.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutRoutingService {

    private final CorridorRepository corridorRepository;
    private final CorridorDeliveryMethodRepository cdmRepository;
    private final PayoutPartnerRepository payoutPartnerRepository;
    private final GatewayRegistry registry;

    /**
     * Most precise resolution: route a SPECIFIC corridor's delivery method to its gateway.
     * A corridor is country-specific, so this is unambiguous even when multiple countries
     * share a currency (XOF, XAF). Prefer this whenever a corridorId is known.
     */
    public PayoutRoute resolveByCorridor(Long corridorId, String deliveryMethod) {
        DeliveryMethod dm = parse(deliveryMethod);
        if (corridorId != null && dm != null) {
            PayoutRoute r = routeForCorridor(corridorId, dm);
            if (r != null) return r;
        }
        return manualRoute();
    }

    /**
     * Country-aware resolution by (receiveCountry, receiveCurrency). When receiveCountry is
     * given it picks the exact corridor for that country; otherwise it falls back to the first
     * active corridor for the currency. Never throws on shared currencies.
     */
    public PayoutRoute resolve(String receiveCountry, String receiveCurrency, String deliveryMethod) {
        DeliveryMethod dm = parse(deliveryMethod);
        if (dm != null) {
            // 1) Exact corridor for the country, if known.
            if (receiveCountry != null && !receiveCountry.isBlank()) {
                CorridorEntity exact = corridorRepository
                        .findFirstByReceiveCountryAndIsActiveTrueOrderByIdAsc(receiveCountry.toUpperCase())
                        .orElse(null);
                if (exact != null) {
                    PayoutRoute r = routeForCorridor(exact.getId(), dm);
                    if (r != null) return r;
                }
            }
            // 2) Fall back to any active corridor for the currency.
            if (receiveCurrency != null) {
                for (CorridorEntity c : corridorRepository.findByReceiveCurrencyAndIsActiveTrue(receiveCurrency.toUpperCase())) {
                    PayoutRoute r = routeForCorridor(c.getId(), dm);
                    if (r != null) return r;
                }
            }
        }
        return manualRoute();
    }

    /** Backward-compatible currency-only resolution (deterministic; used when no country/corridor is known). */
    public PayoutRoute resolve(String receiveCurrency, String deliveryMethod) {
        return resolve(null, receiveCurrency, deliveryMethod);
    }

    /** Build a route from a corridor's active delivery method, or null if none assigned. */
    private PayoutRoute routeForCorridor(Long corridorId, DeliveryMethod dm) {
        for (CorridorDeliveryMethodEntity cdm : cdmRepository.findByCorridorIdAndIsActiveTrue(corridorId)) {
            if (cdm.getDeliveryMethod() == dm && cdm.getPayoutPartnerId() != null) {
                PayoutPartner p = payoutPartnerRepository.findById(cdm.getPayoutPartnerId()).orElse(null);
                if (p != null && Boolean.TRUE.equals(p.getIsActive())) {
                    String gw = (p.getGateway() != null && !p.getGateway().isBlank()) ? p.getGateway() : "MANUAL";
                    return new PayoutRoute(corridorId, p.getId(), p.getPartnerName(),
                            gw, registry.getOrManual(gw).getCapabilities());
                }
            }
        }
        return null;
    }

    private PayoutRoute manualRoute() {
        return new PayoutRoute(null, null, null, "MANUAL",
                registry.getOrManual("MANUAL").getCapabilities());
    }

    private DeliveryMethod parse(String s) {
        if (s == null) return null;
        try { return DeliveryMethod.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
