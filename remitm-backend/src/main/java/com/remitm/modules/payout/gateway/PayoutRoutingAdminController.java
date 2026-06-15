package com.remitm.modules.payout.gateway;

import com.remitm.modules.fx.entity.CorridorDeliveryMethodEntity;
import com.remitm.modules.fx.repository.CorridorDeliveryMethodRepository;
import com.remitm.modules.transaction.entity.PayoutPartner;
import com.remitm.modules.transaction.repository.PayoutPartnerRepository;
import com.remitm.modules.transaction.repository.PayoutTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Admin "Payout Routing" screen backend. Assigns which payout partner (and therefore which gateway)
 * handles each corridor + delivery method.
 *
 * This writes {@code corridor_delivery_methods} — the SINGLE source of truth that
 * {@link PayoutRoutingService} reads. Changing a row here re-routes the corridor's validation AND
 * payout with no code change (e.g. Ghana Mobile Wallet from Nsano → Zeepay = pick a ZEEPAY partner).
 */
@RestController
@RequestMapping("/api/transactions/payout-routing")
@RequiredArgsConstructor
@Slf4j
public class PayoutRoutingAdminController {

    private final CorridorDeliveryMethodRepository deliveryMethodRepository;
    private final PayoutPartnerRepository payoutPartnerRepository;
    private final PayoutTypeRepository payoutTypeRepository;

    /** Every corridor × delivery method, with its currently-assigned partner + resolved gateway. */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        Map<Long, PayoutPartner> partners = new HashMap<>();
        payoutPartnerRepository.findAll().forEach(p -> partners.put(p.getId(), p));

        // Only show corridors that are ENABLED in Transfer Config — i.e. their receive currency has
        // at least one active payout type (the same gate that makes a destination visible to customers).
        // Routing a corridor a customer can't even pick is pointless clutter.
        Set<String> enabledCurrencies = new HashSet<>(payoutTypeRepository.findActiveCurrencies());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CorridorDeliveryMethodEntity dm : deliveryMethodRepository.findAllWithCorridor()) {
            var c = dm.getCorridor();
            if (c.getReceiveCurrency() == null || !enabledCurrencies.contains(c.getReceiveCurrency())) {
                continue;   // corridor not enabled in Transfer Config → hide from routing
            }
            PayoutPartner p = dm.getPayoutPartnerId() != null ? partners.get(dm.getPayoutPartnerId()) : null;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", dm.getId());
            row.put("corridorId", c.getId());
            row.put("sendCurrency", c.getSendCurrency());
            row.put("receiveCurrency", c.getReceiveCurrency());
            row.put("receiveCountry", c.getReceiveCountry());
            row.put("deliveryMethod", dm.getDeliveryMethod() != null ? dm.getDeliveryMethod().name() : null);
            row.put("payoutPartnerId", dm.getPayoutPartnerId());
            row.put("partnerName", p != null ? p.getPartnerName() : null);
            // The gateway is what the partner disburses through — null partner ⇒ MANUAL fallback at runtime.
            row.put("gateway", p != null ? (p.getGateway() != null && !p.getGateway().isBlank() ? p.getGateway() : "MANUAL") : "MANUAL");
            row.put("isActive", dm.getIsActive());
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> r) -> String.valueOf(r.get("receiveCurrency")))
                .thenComparing(r -> String.valueOf(r.get("deliveryMethod"))));
        return ResponseEntity.ok(rows);
    }

    /** Partners available to assign (id, name, gateway) — drives the dropdown. */
    @GetMapping("/partners")
    public ResponseEntity<List<Map<String, Object>>> assignablePartners() {
        List<Map<String, Object>> out = new ArrayList<>();
        payoutPartnerRepository.findAll().forEach(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("partnerName", p.getPartnerName());
            m.put("gateway", p.getGateway() != null && !p.getGateway().isBlank() ? p.getGateway() : "MANUAL");
            out.add(m);
        });
        return ResponseEntity.ok(out);
    }

    /** Assign (or clear, with null) the payout partner for one corridor_delivery_methods row. */
    @PutMapping("/{id}")
    public ResponseEntity<?> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        CorridorDeliveryMethodEntity dm = deliveryMethodRepository.findById(id).orElse(null);
        if (dm == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Routing row not found"));
        }
        Object pid = body.get("payoutPartnerId");
        Long partnerId = (pid != null && !pid.toString().isBlank()) ? Long.valueOf(pid.toString()) : null;
        if (partnerId != null && !payoutPartnerRepository.existsById(partnerId)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unknown payout partner"));
        }
        dm.setPayoutPartnerId(partnerId);
        if (dm.getIsActive() == null) dm.setIsActive(true);
        deliveryMethodRepository.save(dm);
        log.info("Payout routing updated | deliveryMethodId={} -> payoutPartnerId={}", id, partnerId);
        return ResponseEntity.ok(Map.of("success", true, "id", id,
                "payoutPartnerId", partnerId != null ? partnerId : ""));
    }
}
