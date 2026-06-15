package com.remitm.modules.payout.gateway;

import com.remitm.common.enums.ActorType;
import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.TransactionRepository;
import com.remitm.modules.transaction.service.TransactionStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Auto-disburse on funds-received — the missing piece that makes the new flow match the old
 * laylaremitm behaviour exactly:
 *
 * <pre>
 *   customer pays → FUNDS_RECEIVED → [gateway auto-resolved] → auto-disburse via provider API
 *        NSANO  → /deposit → code "00" → PAID (callback also confirms)
 *        ZEEPAY → /api/payouts → code "411" → SENT_TO_PAYOUT → status poll → PAID
 *        MANUAL → skipped (operator pays from the partner portal, as before)
 * </pre>
 *
 * Runs AFTER_COMMIT (the FUNDS_RECEIVED status is durably persisted first) and async (the external
 * provider call never blocks the pay-in request). On failure the transaction is left at
 * FUNDS_RECEIVED so it can be retried or handled manually from the payout portal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoPayoutListener {

    private final TransactionRepository transactionRepository;
    private final GatewayRegistry gatewayRegistry;
    private final PayoutRoutingService payoutRoutingService;
    private final TransactionStateMachine stateMachine;

    /** Master switch — set payout.auto-disburse-enabled=false to revert to manual payout buttons. */
    @Value("${payout.auto-disburse-enabled:true}")
    private boolean autoDisburseEnabled;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPayoutReady(PayoutReadyEvent event) {
        if (!autoDisburseEnabled) return;

        TransactionEntity tx = transactionRepository.findByReferenceNumber(event.referenceNumber()).orElse(null);
        if (tx == null) {
            log.warn("Auto-payout: transaction {} not found", event.referenceNumber());
            return;
        }
        // Only act if it's still freshly funds-received (idempotent against retries / double events).
        if (tx.getStatus() != TransactionStatus.FUNDS_RECEIVED) {
            return;
        }

        // Gateway = the one stamped at creation (immutable); fall back to live resolve if absent.
        String gwType = tx.getPayoutGateway();
        if (gwType == null || gwType.isBlank()) {
            gwType = payoutRoutingService.resolve(
                    tx.getReceiveCurrency(),
                    tx.getDeliveryMethod() != null ? tx.getDeliveryMethod().name() : null
            ).getGateway();
        }

        PayoutGateway gw = gatewayRegistry.getOrManual(gwType);

        // MANUAL / non-API gateways: leave at FUNDS_RECEIVED — an operator completes it from the portal.
        if (!gw.getCapabilities().isAsync()) {
            log.info("Auto-payout: {} is MANUAL ({}); leaving for operator", tx.getReferenceNumber(), gw.getType());
            return;
        }

        try {
            // Stamp the "sent for pay" step (old laylaremitm) — audit row FUNDS_RECEIVED → SENT_TO_PAYOUT
            // before the provider call, so the history is identical even for Nsano (which then → PAID).
            stateMachine.transition(tx, TransactionStatus.SENT_TO_PAYOUT, null, ActorType.SYSTEM,
                    "Auto-routed to " + gw.getType() + " for payout");
            log.info("Auto-payout: disbursing {} via {} (funds received)", tx.getReferenceNumber(), gw.getType());
            Object result = gw.disburse(tx.getReferenceNumber());
            log.info("Auto-payout: {} dispatched via {} -> {}", tx.getReferenceNumber(), gw.getType(), result);
        } catch (Exception ex) {
            // Left at SENT_TO_PAYOUT (the "sent for pay" stamp succeeded) — the Zeepay status poll
            // keeps reconciling it, and the payout partner can mark PAID/FAILED from the portal.
            log.error("Auto-payout FAILED for {} via {}: {}", tx.getReferenceNumber(), gw.getType(), ex.getMessage(), ex);
        }
    }
}
