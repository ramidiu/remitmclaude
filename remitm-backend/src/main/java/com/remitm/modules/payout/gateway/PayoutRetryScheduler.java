package com.remitm.modules.payout.gateway;

import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.payout.nsano.repository.NsanoRepository;
import com.remitm.modules.payout.zeepay.repository.ZeePayRepository;
import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Self-healing for failed payout dispatches. The manual "Retry" button is gone — instead this job
 * periodically re-fires any payout that is stuck at SENT_TO_PAYOUT because its disburse never
 * reached the provider (no provider reference yet). Once the underlying cause is fixed (token
 * refreshed, provider back up) the payout recovers on its own — no human, exactly like the old site
 * where the rail was automatic.
 *
 * <p>Double-pay safe: a transaction is re-disbursed ONLY when it has NO accepted provider id
 * (Zeepay zee_pay_id / Nsano nsano_transaction_id). Anything the provider already accepted is left
 * to {@code ZeepayStatusScheduler} / the Check-Status flow to settle. A time window stops permanently
 * broken transactions from being hammered forever — after it, they need admin attention.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutRetryScheduler {

    private static final List<TransactionStatus> STUCK = List.of(TransactionStatus.SENT_TO_PAYOUT);
    private static final List<String> AUTO_GATEWAYS = List.of("NSANO", "ZEEPAY");

    private final TransactionRepository transactionRepository;
    private final GatewayRegistry gatewayRegistry;
    private final ZeePayRepository zeePayRepository;
    private final NsanoRepository nsanoRepository;

    @Value("${payout.auto-retry-enabled:true}")
    private boolean enabled;

    /** Only retry recent failures; older stuck payouts need manual attention, not endless re-fires. */
    @Value("${payout.auto-retry-window-hours:48}")
    private long windowHours;

    @Scheduled(fixedDelayString = "${payout.auto-retry-interval-ms:300000}")  // every 5 min
    public void retryFailedDispatches() {
        if (!enabled) return;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(windowHours);
        int retried = 0;

        for (String gw : AUTO_GATEWAYS) {
            List<TransactionEntity> stuck =
                    transactionRepository.findByPayoutGatewayAndStatusInOrderByCreatedAtDesc(gw, STUCK);
            for (TransactionEntity tx : stuck) {
                // Bound by CREATED_AT (a genuinely recent failed dispatch), NOT updated_at — otherwise
                // a backfill/edit that bumps updated_at would resurrect old historical payouts and
                // risk DOUBLE-PAYING them. Migrated/historical transactions stay excluded.
                if (tx.getCreatedAt() != null && tx.getCreatedAt().isBefore(cutoff)) continue;
                // skip anything the provider already accepted (has a provider id) — not a failed dispatch
                if (hasProviderId(gw, tx.getReferenceNumber())) continue;

                try {
                    log.info("Auto-retry: re-dispatching {} via {} (no provider ref yet)",
                            tx.getReferenceNumber(), gw);
                    gatewayRegistry.getOrManual(gw).disburse(tx.getReferenceNumber());
                    retried++;
                } catch (Exception ex) {
                    log.warn("Auto-retry failed for {} via {}: {}", tx.getReferenceNumber(), gw, ex.getMessage());
                }
            }
        }
        if (retried > 0) log.info("Auto-retry: re-dispatched {} failed payout(s)", retried);
    }

    /** True if the provider has already accepted this payout (so it must NOT be re-dispatched). */
    private boolean hasProviderId(String gateway, String reference) {
        if ("ZEEPAY".equals(gateway)) {
            return zeePayRepository.findFirstByTransactionIdOrderByIdDesc(reference)
                    .map(z -> z.getZeePayId() != null && !z.getZeePayId().isBlank())
                    .orElse(false);
        }
        if ("NSANO".equals(gateway)) {
            return nsanoRepository.findByTransactionId(reference)
                    .map(n -> n.getNsanoTransactionId() != null && !n.getNsanoTransactionId().isBlank())
                    .orElse(false);
        }
        return false;
    }
}
