package com.remitm.modules.payout.gateway;

import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gateway-agnostic payout facade. The customer form and payout screen call ONLY these endpoints;
 * the active gateway is resolved server-side from corridor_delivery_methods → partner → gateway.
 * Switching a corridor's provider (e.g. Ghana Nsano→Zeepay) needs zero frontend changes.
 */
@RestController
@RequestMapping("/api/payout")
@RequiredArgsConstructor
@Slf4j
public class PayoutFacadeController {

    private final PayoutRoutingService routing;
    private final GatewayRegistry registry;
    private final TransactionRepository transactionRepository;

    /** Which gateway + capabilities apply to a corridor + delivery method (drives the UI). */
    @GetMapping("/route")
    public ResponseEntity<Map<String, Object>> route(@RequestParam String receiveCurrency,
                                                     @RequestParam String deliveryMethod) {
        PayoutRoute r = routing.resolve(receiveCurrency, deliveryMethod);
        return ResponseEntity.ok(Map.of(
                "gateway", r.getGateway(),
                "payoutPartnerId", r.getPayoutPartnerId() != null ? r.getPayoutPartnerId() : "",
                "partnerName", r.getPartnerName() != null ? r.getPartnerName() : "",
                "supportsNameCheck", r.getCapabilities().isSupportsNameCheck(),
                "async", r.getCapabilities().isAsync()));
    }

    /** Generic recipient validation — resolves the gateway and dispatches; UI never names one. */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestParam String receiveCurrency,
                                                        @RequestParam String deliveryMethod,
                                                        @RequestParam String accountNumber,
                                                        @RequestParam(required = false) String bankOrProvider,
                                                        @RequestParam(required = false) String routingNumber) {
        PayoutRoute r = routing.resolve(receiveCurrency, deliveryMethod);
        PayoutGateway gw = registry.getOrManual(r.getGateway());
        if (!gw.getCapabilities().isSupportsNameCheck()) {
            return ResponseEntity.ok(Map.of("found", false, "accountName", "",
                    "gateway", gw.getType(), "supported", false));
        }
        ValidationResult vr = gw.validateRecipient(ValidationRequest.builder()
                .deliveryMethod(deliveryMethod)
                .accountNumber(accountNumber)
                .bankOrProvider(bankOrProvider)
                .routingNumber(routingNumber)
                .receivingCountry(isoForCurrency(receiveCurrency))
                .build());
        return ResponseEntity.ok(Map.of(
                "found", vr.isFound(),
                "accountName", vr.getAccountName() != null ? vr.getAccountName() : "",
                "gateway", gw.getType(), "supported", true));
    }

    /** Generic payout — resolves the gateway from the transaction's stamped/corridor route. */
    @PostMapping("/disburse/{referenceNumber}")
    public ResponseEntity<?> disburse(@PathVariable String referenceNumber) {
        TransactionEntity tx = transactionRepository.findByReferenceNumber(referenceNumber).orElse(null);
        if (tx == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Transaction not found"));
        }
        String gwType = tx.getPayoutGateway();
        if (gwType == null || gwType.isBlank()) {
            gwType = routing.resolve(tx.getReceiveCurrency(),
                    tx.getDeliveryMethod() != null ? tx.getDeliveryMethod().name() : null).getGateway();
        }
        PayoutGateway gw = registry.getOrManual(gwType);
        try {
            return ResponseEntity.ok(gw.disburse(referenceNumber));
        } catch (Exception ex) {
            log.error("Payout disburse failed | ref={} gateway={}", referenceNumber, gwType, ex);
            return ResponseEntity.status(500).body(Map.of("success", false, "gateway", gwType,
                    "message", "Internal error: " + ex.getMessage()));
        }
    }

    /** Receive currency → ISO-2 country code (Zeepay/Nsano expect a country). */
    private String isoForCurrency(String currency) {
        if (currency == null) return "";
        switch (currency.toUpperCase()) {
            case "GHS": return "GH";
            case "ZWL": case "ZWG": return "ZW";
            case "ZMW": return "ZM";
            case "NGN": return "NG";
            case "KES": return "KE";
            case "TZS": return "TZ";
            case "UGX": return "UG";
            default: return "";
        }
    }
}
