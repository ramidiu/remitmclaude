package com.remitm.modules.payout.nsano.scheduler;

import com.remitm.modules.payout.nsano.config.NsanoProperties;
import com.remitm.modules.payout.nsano.entity.NsanoEntity;
import com.remitm.modules.payout.nsano.repository.NsanoRepository;
import com.remitm.modules.payout.nsano.service.NsanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls NSANO for the status of in-flight payouts.
 *
 * Disabled by default; the entire method is guarded by {@code nsano.scheduler-enabled}.
 * Finds NSANO records that are not in a terminal state (Paid/PAID/FAILED) and have an
 * NSANO transaction id, polls each, and on SUCCESS marks the transaction PAID + record Paid.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NsanoStatusScheduler {

    private static final List<String> TERMINAL_STATUSES = List.of("Paid", "PAID", "FAILED");

    private final NsanoProperties properties;
    private final NsanoRepository nsanoRepository;
    private final NsanoService nsanoService;

    @Scheduled(fixedDelayString = "${nsano.status-poll-interval-ms:300000}")
    public void pollPendingPayouts() {
        if (!properties.isSchedulerEnabled()) {
            log.debug("NSANO status scheduler disabled — skipping");
            return;
        }
        try {
            List<NsanoEntity> pending =
                    nsanoRepository.findByStatusNotInAndNsanoTransactionIdIsNotNull(TERMINAL_STATUSES);
            if (pending.isEmpty()) {
                log.debug("NSANO status scheduler: nothing to poll");
                return;
            }
            log.info("NSANO status scheduler: polling {} payout(s)", pending.size());
            for (NsanoEntity record : pending) {
                try {
                    nsanoService.pollStatus(record);
                } catch (Exception ex) {
                    log.warn("NSANO status scheduler: poll failed for record id={} (ref={})",
                            record.getId(), record.getTransactionId(), ex);
                }
            }
        } catch (Exception ex) {
            log.error("NSANO status scheduler: EXCEPTION", ex);
        }
    }
}
