package com.remitm.modules.payin.fire.controller;

import com.remitm.common.enums.ActorType;
import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.payin.fire.dto.FireDtos.PaymentRequestBody;
import com.remitm.modules.payin.fire.dto.FireDtos.PaymentRequestResponse;
import com.remitm.modules.payin.fire.dto.FireDtos.UpdatePaymentBody;
import com.remitm.modules.payin.fire.entity.FirePaymentEntity;
import com.remitm.modules.payin.fire.repository.FirePaymentRepository;
import com.remitm.modules.payin.fire.service.FireService;
import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.TransactionRepository;
import com.remitm.modules.transaction.service.TransactionStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST endpoints for the Fire (fire.com) Open Banking pay-in flow.
 */
@RestController
@RequestMapping("/api/fire")
@RequiredArgsConstructor
@Slf4j
public class FireController {

    private final FireService fireService;
    private final FirePaymentRepository firePaymentRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionStateMachine stateMachine;

    /**
     * Creates a Fire hosted payment request for a transaction and returns the
     * URL the payer should be redirected to.
     */
    @PostMapping("/payment-request")
    @Transactional
    public ResponseEntity<?> createPaymentRequest(@RequestBody PaymentRequestBody body) {
        String referenceNumber = body != null ? body.getReferenceNumber() : null;
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "referenceNumber is required"));
        }

        try {
            Optional<TransactionEntity> txOpt = transactionRepository.findByReferenceNumber(referenceNumber);
            if (txOpt.isEmpty()) {
                log.warn("Fire payment-request: transaction not found for ref={}", referenceNumber);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Transaction not found"));
            }
            TransactionEntity tx = txOpt.get();

            String accessToken = fireService.getAccessToken();
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Unable to obtain Fire access token"));
            }

            String description = "RemitM payment " + referenceNumber;
            String code = fireService.createPaymentRequest(accessToken, tx, description);
            if (code == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Unable to create Fire payment request"));
            }

            FirePaymentEntity entity = FirePaymentEntity.builder()
                    .transactionId(referenceNumber)
                    .fireCode(code)
                    .icanTo(fireService.getProps().getIcanTo())
                    .currency("GBP")
                    .amount(tx.getSendAmount())
                    .myReference(referenceNumber)
                    .description(description)
                    .returnUrl(fireService.getProps().getReturnUrl())
                    .status("PENDING")
                    .build();
            firePaymentRepository.save(entity);

            String url = fireService.buildHostedUrl(code);
            return ResponseEntity.ok(new PaymentRequestResponse(url, code));
        } catch (Exception e) {
            log.error("Fire payment-request failed for ref={}: {}", referenceNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create payment request"));
        }
    }

    /**
     * Called by the frontend when it returns from Fire after a successful
     * payment. Marks the Fire payment record SUCCESS and advances the
     * transaction PENDING -> PROCESSING.
     */
    @PostMapping("/update-payment")
    @Transactional
    public ResponseEntity<?> updatePayment(@RequestBody UpdatePaymentBody body) {
        String referenceNumber = body != null ? body.getReferenceNumber() : null;
        String paymentUuid = body != null ? body.getPaymentUuid() : null;

        if (referenceNumber == null || referenceNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "referenceNumber is required"));
        }

        log.info("Fire update-payment: ref={} paymentUuid={}", referenceNumber, paymentUuid);

        try {
            // Update the Fire payment record.
            Optional<FirePaymentEntity> fireOpt = firePaymentRepository.findByTransactionId(referenceNumber);
            if (fireOpt.isPresent()) {
                FirePaymentEntity fire = fireOpt.get();
                fire.setStatus("SUCCESS");
                if (paymentUuid != null && !paymentUuid.isBlank()) {
                    fire.setPaymentUuid(paymentUuid);
                }
                firePaymentRepository.save(fire);
            } else {
                log.warn("Fire update-payment: no fire_payments row for ref={}", referenceNumber);
            }

            // Update the transaction.
            Optional<TransactionEntity> txOpt = transactionRepository.findByReferenceNumber(referenceNumber);
            if (txOpt.isPresent()) {
                TransactionEntity tx = txOpt.get();
                if (paymentUuid != null && !paymentUuid.isBlank()) {
                    tx.setPaymentReference(paymentUuid);
                }
                try {
                    if (tx.getStatus() == TransactionStatus.PENDING) {
                        tx = stateMachine.transition(tx, TransactionStatus.PROCESSING,
                                0L, ActorType.SYSTEM,
                                "Open Banking payment received via Fire. UUID: " + paymentUuid);
                    } else {
                        // Persist the payment reference even if not in PENDING.
                        transactionRepository.save(tx);
                    }
                    log.info("Transaction {} updated to {}", referenceNumber, tx.getStatus());
                } catch (Exception e) {
                    // Fallback: keep the payment reference, leave status as-is (PENDING).
                    log.error("Failed to transition transaction {} to PROCESSING: {}",
                            referenceNumber, e.getMessage(), e);
                    transactionRepository.save(tx);
                }
            } else {
                log.warn("Fire update-payment: transaction not found for ref={}", referenceNumber);
            }

            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Fire update-payment failed for ref={}: {}", referenceNumber, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update payment"));
        }
    }

    /**
     * Passthrough: list the supported ASPSPs (banks) for GBP Open Banking.
     */
    @GetMapping("/aspsps")
    public ResponseEntity<?> listAspsps() {
        try {
            String accessToken = fireService.getAccessToken();
            if (accessToken == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Unable to obtain Fire access token"));
            }
            Map<String, Object> aspsps = fireService.listAspsps(accessToken);
            if (aspsps == null) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Unable to fetch bank list"));
            }
            return ResponseEntity.ok(aspsps);
        } catch (Exception e) {
            log.error("Fire aspsps failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch bank list"));
        }
    }
}
