package com.remitm.modules.payout.nsano.service;

import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.payout.nsano.config.NsanoProperties;
import com.remitm.modules.payout.nsano.dto.NsanoApiResponse;
import com.remitm.modules.payout.nsano.dto.NsanoInitiateRequest;
import com.remitm.modules.payout.nsano.entity.NsanoEntity;
import com.remitm.modules.payout.nsano.repository.NsanoRepository;
import com.remitm.common.enums.DeliveryMethod;
import com.remitm.modules.transaction.entity.BeneficiaryEntity;
import com.remitm.modules.transaction.entity.TransactionEntity;
import com.remitm.modules.transaction.repository.BeneficiaryRepository;
import com.remitm.modules.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * NSANO Ghana payout integration (mobile-money / bank disbursement).
 *
 * Auth is via custom headers on EVERY request (Authorization-Key, not bearer).
 * Success is indicated by NSANO code "00"; status "SUCCESS" / callback status
 * "SUCESSFULL" (NSANO's misspelling) map our transaction to PAID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NsanoService {

    private static final String SUCCESS_CODE = "00";
    private static final String RECIPIENT_COUNTRY = "GH";

    private final NsanoProperties properties;
    private final NsanoRepository nsanoRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // ------------------------------------------------------------------ headers

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization-Key", properties.getApiKey());
        headers.set("User-Agent", "Application");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    // ------------------------------------------------------------------ initiate

    /**
     * Initiate a payout for the given transaction. Persists/updates the NsanoEntity
     * and, on NSANO code "00", stores the returned NSANO transaction id for polling.
     */
    @Transactional
    public NsanoApiResponse initiate(NsanoInitiateRequest request) {
        String ref = request.getReferenceNumber();
        log.info("========== NSANO initiate START | ref={} ==========", ref);

        TransactionEntity tx = transactionRepository.findByReferenceNumber(ref).orElse(null);
        if (tx == null) {
            log.warn("NSANO: transaction not found | ref={}", ref);
            return errorResponse("Transaction not found: " + ref);
        }

        boolean isWallet = !"BANK".equalsIgnoreCase(request.getPaymentType());
        String endpoint = properties.getBaseUrl()
                + (isWallet ? "/api/v2/remittance/deposit/wallet"
                            : "/api/v2/remittance/deposit/account");

        // Persist (or reuse) the NSANO record before the call so we always have a trail.
        NsanoEntity record = nsanoRepository.findByTransactionId(ref)
                .orElseGet(() -> NsanoEntity.builder().transactionId(ref).build());
        record.setSenderName(tx.getSenderName());
        record.setRecipientName(request.getRecipientName());
        record.setSenderAccount(tx.getReferenceNumber());
        record.setRecipientAccount(request.getRecipient());
        record.setSourceCurrency(tx.getSendCurrency());
        record.setDestCurrency(tx.getReceiveCurrency());
        record.setAmount(tx.getReceiveAmount());
        record.setRate(tx.getAppliedRate());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("senderName", tx.getSenderName());
        body.put("sourceCurrency", tx.getSendCurrency());
        body.put("recipientName", request.getRecipientName());
        body.put("recipientCountry", RECIPIENT_COUNTRY);
        body.put("reference", tx.getReferenceNumber());
        body.put("amount", tx.getReceiveAmount());
        body.put("exchangeRate", tx.getAppliedRate());
        body.put("narration", request.getNarration());
        body.put("destinationHouse", request.getDestinationHouse());
        // Old laylaremitm performDeposit set sender == the recipient's account / mobile number.
        body.put("sender", request.getRecipient());
        body.put("recipient", request.getRecipient());

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders());
            log.info("NSANO: POST {} | ref={}", endpoint, ref);
            ResponseEntity<NsanoApiResponse> response =
                    restTemplate.postForEntity(endpoint, entity, NsanoApiResponse.class);

            NsanoApiResponse apiResponse = response.getBody();
            log.info("NSANO: HTTP {} | code={} | ref={}", response.getStatusCode(),
                    apiResponse != null ? apiResponse.getCode() : "null", ref);

            if (apiResponse != null) {
                record.setCode(apiResponse.getCode());
                record.setMessage(apiResponse.getMsg());
                if (SUCCESS_CODE.equals(apiResponse.getCode())) {
                    if (apiResponse.getData() != null) {
                        record.setNsanoTransactionId(apiResponse.getData().getTransactionId());
                    }
                    record.setStatus("PENDING");
                    record.setApiStatus("done");
                } else {
                    record.setStatus("FAILED");
                    record.setApiStatus("done");
                }
            }
            nsanoRepository.save(record);
            log.info("========== NSANO initiate END | ref={} | nsanoId={} ==========",
                    ref, record.getNsanoTransactionId());
            return apiResponse != null ? apiResponse : errorResponse("Empty NSANO response");

        } catch (Exception ex) {
            log.error("NSANO: initiate EXCEPTION | ref={}", ref, ex);
            record.setStatus("FAILED");
            record.setApiStatus("error");
            record.setMessage(ex.getMessage());
            try {
                nsanoRepository.save(record);
            } catch (Exception saveEx) {
                log.error("NSANO: failed to persist error record | ref={}", ref, saveEx);
            }
            return errorResponse("NSANO initiate failed: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ auto-disburse

    /**
     * Auto-disburse a transaction via NSANO, building the request from the transaction's
     * BENEFICIARY exactly like the old laylaremitm {@code performDeposit}:
     *   Bank   -> destinationHouse = SWIFT/BIC, recipient = account number, deposit/account
     *   Wallet -> destinationHouse = mobile provider, recipient = mobile number, deposit/wallet
     * On NSANO success code "00" the transaction is marked PAID immediately (old behaviour).
     * This is what the payout-partner portal's "Pay via Nsano" button calls — the operator
     * no longer types recipient details by hand.
     */
    @Transactional
    public NsanoApiResponse disburse(String referenceNumber) {
        log.info("========== NSANO disburse (auto) START | ref={} ==========", referenceNumber);
        TransactionEntity tx = transactionRepository.findByReferenceNumber(referenceNumber).orElse(null);
        if (tx == null) {
            return errorResponse("Transaction not found: " + referenceNumber);
        }
        // NSANO disburses to Ghana only (the old website hardcoded recipientCountry = "GH").
        if (tx.getReceiveCurrency() != null && !"GHS".equalsIgnoreCase(tx.getReceiveCurrency())) {
            return errorResponse("NSANO payout is Ghana (GHS) only; transaction "
                    + referenceNumber + " receives " + tx.getReceiveCurrency());
        }
        if (tx.getBeneficiaryId() == null) {
            return errorResponse("No beneficiary on transaction " + referenceNumber);
        }
        BeneficiaryEntity bene = beneficiaryRepository.findById(tx.getBeneficiaryId()).orElse(null);
        if (bene == null) {
            return errorResponse("Beneficiary not found for transaction " + referenceNumber);
        }

        NsanoInitiateRequest req = new NsanoInitiateRequest();
        req.setReferenceNumber(referenceNumber);
        req.setRecipientName(bene.getFullName());
        req.setNarration("Remittance payout for " + referenceNumber);

        DeliveryMethod dm = bene.getDeliveryMethod() != null ? bene.getDeliveryMethod() : tx.getDeliveryMethod();
        if (dm == DeliveryMethod.BANK_DEPOSIT) {
            req.setPaymentType("BANK");
            req.setDestinationHouse(bene.getSwiftBic());
            req.setRecipient(bene.getAccountNumber());
        } else {
            req.setPaymentType("WALLET");
            req.setDestinationHouse(bene.getMobileProvider());
            req.setRecipient(bene.getMobileNumber());
        }
        log.info("NSANO disburse: type={} destinationHouse={} recipient={} ref={}",
                req.getPaymentType(), req.getDestinationHouse(), req.getRecipient(), referenceNumber);

        NsanoApiResponse resp = initiate(req);

        // Old website: on code "00" the transaction goes straight to Paid.
        if (resp != null && SUCCESS_CODE.equals(resp.getCode())) {
            nsanoRepository.findByTransactionId(referenceNumber).ifPresent(this::markPaid);
        }
        log.info("========== NSANO disburse (auto) END | ref={} | code={} ==========",
                referenceNumber, resp != null ? resp.getCode() : "null");
        return resp;
    }

    // ------------------------------------------------------------------ status

    /** Current persisted status for a transaction reference (no remote call). */
    public Map<String, Object> currentStatus(String referenceNumber) {
        Optional<NsanoEntity> record = nsanoRepository.findByTransactionId(referenceNumber);
        if (record.isEmpty()) {
            return Map.of("found", false, "message", "No NSANO record for " + referenceNumber);
        }
        NsanoEntity r = record.get();
        Map<String, Object> out = new HashMap<>();
        out.put("found", true);
        out.put("referenceNumber", r.getTransactionId());
        out.put("nsanoTransactionId", nvl(r.getNsanoTransactionId()));
        out.put("status", nvl(r.getStatus()));
        out.put("apiStatus", nvl(r.getApiStatus()));
        out.put("code", nvl(r.getCode()));
        out.put("message", nvl(r.getMessage()));
        return out;
    }

    /**
     * Poll NSANO for the live status of a record and, on "SUCCESS", mark both the
     * transaction PAID and the record Paid. Returns the (possibly updated) record.
     */
    @Transactional
    public NsanoEntity pollStatus(NsanoEntity record) {
        String nsanoId = record.getNsanoTransactionId();
        if (nsanoId == null || nsanoId.isBlank()) {
            return record;
        }
        String url = properties.getBaseUrl() + "/api/v2/remittance/transactions/" + nsanoId;
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<NsanoApiResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, NsanoApiResponse.class);
            NsanoApiResponse apiResponse = response.getBody();
            if (apiResponse != null && apiResponse.getData() != null) {
                NsanoApiResponse.NsanoData data = apiResponse.getData();
                if (data.getMsg() != null) record.setMessage(data.getMsg());
                if (data.getCode() != null) record.setCode(data.getCode());
                if ("SUCCESS".equalsIgnoreCase(data.getStatus())) {
                    markPaid(record);
                } else if (data.getStatus() != null) {
                    record.setStatus(data.getStatus());
                }
            }
            nsanoRepository.save(record);
        } catch (Exception ex) {
            log.error("NSANO: status poll EXCEPTION | nsanoId={}", nsanoId, ex);
        }
        return record;
    }

    // ------------------------------------------------------------------ callback

    /**
     * Inbound NSANO callback. On status "SUCESSFULL" (NSANO's misspelling — matched
     * exactly) mark the transaction PAID and the record Paid. Correlate by reference.
     */
    @Transactional
    public void handleCallback(String reference, String transactionId, String status, String msg) {
        log.info("NSANO callback | reference={} | transactionId={} | status={} | msg={}",
                reference, transactionId, status, msg);

        NsanoEntity record = nsanoRepository.findByTransactionId(reference)
                .or(() -> transactionId != null
                        ? nsanoRepository.findByNsanoTransactionId(transactionId)
                        : Optional.empty())
                .orElse(null);

        if (record == null) {
            log.warn("NSANO callback: no record for reference={} transactionId={}", reference, transactionId);
            return;
        }

        if (msg != null) record.setMessage(msg);
        if (transactionId != null && (record.getNsanoTransactionId() == null
                || record.getNsanoTransactionId().isBlank())) {
            record.setNsanoTransactionId(transactionId);
        }

        if ("SUCESSFULL".equals(status)) {
            markPaid(record);
        } else if (status != null) {
            record.setStatus(status);
        }
        nsanoRepository.save(record);
    }

    // ------------------------------------------------------------------ helpers

    /** Mark both the NSANO record and the underlying transaction as paid. */
    private void markPaid(NsanoEntity record) {
        record.setStatus("Paid");
        record.setApiStatus("done");
        transactionRepository.findByReferenceNumber(record.getTransactionId()).ifPresent(tx -> {
            if (tx.getStatus() != TransactionStatus.PAID && tx.getStatus() != TransactionStatus.COMPLETED) {
                tx.setStatus(TransactionStatus.PAID);
                tx.setPayoutReference(record.getNsanoTransactionId());
                tx.setPayoutConfirmedAt(LocalDateTime.now());
                tx.setUpdatedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                log.info("NSANO: transaction {} marked PAID (nsanoId={})",
                        tx.getReferenceNumber(), record.getNsanoTransactionId());
            }
        });
    }

    private NsanoApiResponse errorResponse(String message) {
        NsanoApiResponse r = new NsanoApiResponse();
        r.setCode("99");
        r.setMsg(message);
        return r;
    }

    private String nvl(String v) {
        return v != null ? v : "";
    }

    // expose a couple of values for the scheduler / name-check helpers

    public NsanoProperties getProperties() {
        return properties;
    }

    /**
     * Optional name-check helper: returns the resolved account name on code "00", else null.
     */
    public String nameCheck(boolean wallet, String accountNumber, String destinationHouse) {
        String url = properties.getBaseUrl()
                + (wallet ? "/api/v2/remittance/name-check/wallet"
                          : "/api/v2/remittance/name-check/account");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("accountNumber", accountNumber);
        body.put("destinationHouse", destinationHouse);
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, authHeaders());
            ResponseEntity<NsanoApiResponse> response =
                    restTemplate.postForEntity(url, entity, NsanoApiResponse.class);
            NsanoApiResponse r = response.getBody();
            if (r != null && SUCCESS_CODE.equals(r.getCode()) && r.getData() != null) {
                return r.getData().getAccountName();
            }
        } catch (Exception ex) {
            log.error("NSANO: name-check EXCEPTION | account={}", accountNumber, ex);
        }
        return null;
    }
}
