package com.remitm.modules.payout.zeepay.scheduler;

import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.payout.zeepay.config.ZeepayConfig;
import com.remitm.modules.payout.zeepay.entity.ZeePayEntity;
import com.remitm.modules.payout.zeepay.repository.ZeePayRepository;
import com.remitm.modules.payout.zeepay.service.ZeepayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pull-only status reconciliation: Zeepay has no inbound callback, so we periodically poll
 * in-flight payouts (status PENDING / SENT_TO_PAYOUT with a zee_pay_id) until they settle.
 *
 * <p>Guarded by {@code zeepay.scheduler-enabled} (default false). The bean always exists so
 * the scheduled method is registered, but it short-circuits when the flag is off.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ZeepayStatusScheduler {

    private static final List<String> IN_FLIGHT_STATUSES = List.of(
            "PENDING", TransactionStatus.SENT_TO_PAYOUT.name());

    private final ZeepayConfig config;
    private final ZeePayRepository zeePayRepository;
    private final ZeepayService zeepayService;

    @Scheduled(fixedDelayString = "${zeepay.status-poll-interval-ms:30000}")
    public void pollInFlightPayouts() {
        if (!config.isSchedulerEnabled()) {
            return;
        }

        List<ZeePayEntity> pending = zeePayRepository
                .findByZeePayIdIsNotNullAndStatusIn(IN_FLIGHT_STATUSES);
        if (pending.isEmpty()) {
            return;
        }

        log.info("Zeepay status poll: checking {} in-flight payout(s)", pending.size());
        for (ZeePayEntity record : pending) {
            try {
                zeepayService.pollAndApply(record);
            } catch (Exception e) {
                log.warn("Zeepay status poll failed for extrId={}: {}",
                        record.getExtraId(), e.getMessage());
            }
        }
    }
}
