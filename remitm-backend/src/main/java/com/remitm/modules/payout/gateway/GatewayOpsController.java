package com.remitm.modules.payout.gateway;

import com.remitm.common.enums.ActorType;
import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.payout.nsano.repository.NsanoRepository;
import com.remitm.modules.payout.nsano.service.NsanoService;
import com.remitm.modules.payout.zeepay.repository.ZeePayRepository;
import com.remitm.modules.payout.zeepay.service.ZeepayService;
import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.BeneficiaryRepository;
import com.remitm.modules.transaction.repository.TransactionRepository;
import com.remitm.modules.transaction.service.TransactionStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Per-gateway operations screen (one admin page per API rail — Nsano, Zeepay).
 *
 * <p>Mirrors the old laylaremitm per-provider dashboards: each page shows only that gateway's
 * transactions, that gateway's reconciliation IDs, and that gateway's actions (retry / mark paid).
 * The operator can never pick the wrong rail — the page IS the rail. MANUAL has no page (it's the
 * normal pay-in/pay-out + admin mark-paid flow).
 */
@RestController
@RequestMapping("/api/payout/gateway")
@RequiredArgsConstructor
@Slf4j
public class GatewayOpsController {

    private static final List<TransactionStatus> PENDING = List.of(
            TransactionStatus.PROCESSING, TransactionStatus.FUNDS_RECEIVED, TransactionStatus.SENT_TO_PAYOUT);
    // Completed view includes ARCHIVED so historical/dumped payouts (the migrated ones) show too.
    private static final List<TransactionStatus> DONE = List.of(
            TransactionStatus.PAID, TransactionStatus.COMPLETED, TransactionStatus.ARCHIVED);
    // Cancelled/Failed view — terminal, non-actionable; shown read-only for visibility.
    private static final List<TransactionStatus> CANCELLED = List.of(
            TransactionStatus.CANCELLED, TransactionStatus.FAILED, TransactionStatus.REFUNDED);

    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ZeePayRepository zeePayRepository;
    private final NsanoRepository nsanoRepository;
    private final TransactionStateMachine stateMachine;
    private final ZeepayService zeepayService;
    private final NsanoService nsanoService;

    /** One page of a gateway's transactions (scope-filtered + server-side search) + a totals summary. */
    @GetMapping("/{gateway}/transactions")
    public ResponseEntity<Map<String, Object>> list(@PathVariable String gateway,
                                                     @RequestParam(defaultValue = "all") String scope,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "25") int size,
                                                     @RequestParam(required = false) String search) {
        String gw = gateway.toUpperCase();
        String q = (search != null && !search.isBlank()) ? search.trim() : null;   // null => no filter
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by("createdAt").descending());

        // "all" = every status (find any transaction's latest status); else status-bucketed.
        Page<TransactionEntity> pageData;
        if ("all".equalsIgnoreCase(scope)) {
            pageData = transactionRepository.pageGatewayAll(gw, q, pageable);
        } else {
            List<TransactionStatus> wanted = "done".equalsIgnoreCase(scope) ? DONE
                    : "cancelled".equalsIgnoreCase(scope) ? CANCELLED
                    : PENDING;
            pageData = transactionRepository.pageGatewayScoped(gw, wanted, q, pageable);
        }

        // Summary from COUNT/SUM queries (cheap, accurate, independent of pagination).
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("pending", transactionRepository.countByPayoutGatewayAndStatusIn(gw, PENDING));
        summary.put("paid", transactionRepository.countByPayoutGatewayAndStatusIn(gw, DONE));
        summary.put("cancelled", transactionRepository.countByPayoutGatewayAndStatusIn(gw, CANCELLED));
        summary.put("paidTotalAmount", transactionRepository.sumReceiveAmountByGatewayAndStatusIn(gw, DONE));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("gateway", gw);
        body.put("scope", scope);
        body.put("summary", summary);
        body.put("transactions", pageData.getContent().stream().map(t -> enrich(t, gw)).toList());
        body.put("page", pageData.getNumber());
        body.put("size", pageData.getSize());
        body.put("totalElements", pageData.getTotalElements());
        body.put("totalPages", pageData.getTotalPages());
        return ResponseEntity.ok(body);
    }

    /**
     * Ask the PROVIDER for the real, current status of a payout and apply it. This is the correct
     * way to settle an automatic gateway — the truth comes from Nsano/Zeepay, never a human button.
     * On a confirmed "Success/SUCCESS" the provider service marks the transaction PAID.
     */
    @PostMapping("/check-status/{referenceNumber}")
    public ResponseEntity<Map<String, Object>> checkStatus(@PathVariable String referenceNumber) {
        TransactionEntity tx = transactionRepository.findByReferenceNumber(referenceNumber).orElse(null);
        if (tx == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Transaction not found"));
        }
        String gw = tx.getPayoutGateway() != null ? tx.getPayoutGateway().toUpperCase() : "";
        try {
            if ("ZEEPAY".equals(gw)) {
                zeepayService.checkStatus(referenceNumber);
            } else if ("NSANO".equals(gw)) {
                nsanoRepository.findByTransactionId(referenceNumber).ifPresent(nsanoService::pollStatus);
            } else {
                return ResponseEntity.ok(Map.of("success", false,
                        "message", "No live status check for gateway " + gw));
            }
            TransactionEntity fresh = transactionRepository.findByReferenceNumber(referenceNumber).orElse(tx);
            return ResponseEntity.ok(Map.of("success", true,
                    "status", fresh.getStatus() != null ? fresh.getStatus().name() : null));
        } catch (Exception ex) {
            log.error("Gateway status-check failed for {} ({}): {}", referenceNumber, gw, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    /** Mark a stuck transaction PAID from the gateway page (provider confirmed out-of-band / reconciled). */
    @PutMapping("/mark-paid/{referenceNumber}")
    public ResponseEntity<Map<String, Object>> markPaid(@PathVariable String referenceNumber) {
        TransactionEntity tx = transactionRepository.findByReferenceNumber(referenceNumber).orElse(null);
        if (tx == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Transaction not found"));
        }
        try {
            // FUNDS_RECEIVED must pass through SENT_TO_PAYOUT before PAID (state machine rule).
            if (tx.getStatus() == TransactionStatus.FUNDS_RECEIVED) {
                tx = stateMachine.transition(tx, TransactionStatus.SENT_TO_PAYOUT, null, ActorType.ADMIN,
                        "Gateway ops: routing to payout before mark-paid");
            }
            if (tx.getStatus() != TransactionStatus.PAID && tx.getStatus() != TransactionStatus.COMPLETED) {
                tx = stateMachine.transition(tx, TransactionStatus.PAID, null, ActorType.ADMIN,
                        "Marked paid from " + (tx.getPayoutGateway() != null ? tx.getPayoutGateway() : "gateway") + " operations page");
            }
            return ResponseEntity.ok(Map.of("success", true, "status", tx.getStatus().name()));
        } catch (Exception ex) {
            log.error("Gateway mark-paid failed for {}: {}", referenceNumber, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    private Map<String, Object> enrich(TransactionEntity tx, String gw) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", tx.getId());
        m.put("referenceNumber", tx.getReferenceNumber());
        m.put("status", tx.getStatus() != null ? tx.getStatus().name() : null);
        m.put("deliveryMethod", tx.getDeliveryMethod() != null ? tx.getDeliveryMethod().name() : null);
        m.put("receiveAmount", tx.getReceiveAmount());
        m.put("receiveCurrency", tx.getReceiveCurrency());
        m.put("createdAt", tx.getCreatedAt());
        m.put("beneficiaryName", tx.getSenderName()); // fallback; replaced below if beneficiary found
        if (tx.getBeneficiaryId() != null) {
            beneficiaryRepository.findById(tx.getBeneficiaryId()).ifPresent(b -> {
                m.put("beneficiaryName", b.getFullName());
                m.put("beneficiaryAccountNumber", b.getAccountNumber());
                m.put("beneficiaryMobileNumber", b.getMobileNumber());
                m.put("beneficiaryBankName", b.getBankName());
            });
        }
        // Provider reconciliation ID — the value an ops person matches against the provider portal.
        String providerRef = null;
        if ("ZEEPAY".equals(gw)) {
            providerRef = zeePayRepository.findFirstByTransactionIdOrderByIdDesc(tx.getReferenceNumber())
                    .map(z -> z.getZeePayId() != null ? z.getZeePayId() : z.getExtraId()).orElse(null);
        } else if ("NSANO".equals(gw)) {
            providerRef = nsanoRepository.findByTransactionId(tx.getReferenceNumber())
                    .map(n -> n.getNsanoTransactionId()).orElse(null);
        }
        m.put("providerRef", providerRef);
        return m;
    }
}
