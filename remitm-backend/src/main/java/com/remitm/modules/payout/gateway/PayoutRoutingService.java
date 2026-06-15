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

    public PayoutRoute resolve(String receiveCurrency, String deliveryMethod) {
        DeliveryMethod dm = parse(deliveryMethod);
        if (receiveCurrency != null && dm != null) {
            List<CorridorEntity> corridors =
                    corridorRepository.findByReceiveCurrencyAndIsActiveTrue(receiveCurrency.toUpperCase());
            for (CorridorEntity c : corridors) {
                for (CorridorDeliveryMethodEntity cdm : cdmRepository.findByCorridorIdAndIsActiveTrue(c.getId())) {
                    if (cdm.getDeliveryMethod() == dm && cdm.getPayoutPartnerId() != null) {
                        PayoutPartner p = payoutPartnerRepository.findById(cdm.getPayoutPartnerId()).orElse(null);
                        if (p != null && Boolean.TRUE.equals(p.getIsActive())) {
                            String gw = (p.getGateway() != null && !p.getGateway().isBlank())
                                    ? p.getGateway() : "MANUAL";
                            return new PayoutRoute(c.getId(), p.getId(), p.getPartnerName(),
                                    gw, registry.getOrManual(gw).getCapabilities());
                        }
                    }
                }
            }
        }
        // Nothing routed → MANUAL (no live gateway).
        return new PayoutRoute(null, null, null, "MANUAL",
                registry.getOrManual("MANUAL").getCapabilities());
    }

    private DeliveryMethod parse(String s) {
        if (s == null) return null;
        try { return DeliveryMethod.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
