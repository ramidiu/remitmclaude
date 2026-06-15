package com.remitm.modules.payout.zeepay.service;

import com.remitm.common.enums.TransactionStatus;
import com.remitm.modules.auth.entity.UserEntity;
import com.remitm.modules.auth.repository.UserRepository;
import com.remitm.modules.payout.zeepay.config.ZeepayConfig;
import com.remitm.modules.payout.zeepay.dto.ZeepayInitiateRequest;
import com.remitm.modules.payout.zeepay.dto.ZeepayInitiateResponse;
import com.remitm.modules.payout.zeepay.dto.ZeepayServiceType;
import com.remitm.modules.payout.zeepay.dto.ZeepayStatusResponse;
import com.remitm.modules.payout.zeepay.entity.ZeePayEntity;
import com.remitm.modules.payout.zeepay.repository.ZeePayRepository;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Zeepay payout integration (mobile-money / bank / cash-pickup disbursement).
 *
 * <p>Auth is a static long-lived Bearer JWT on every call. Initiate is a single
 * {@code POST api/payouts} multipart endpoint shared by all three service types. There is
 * no inbound callback — completion is discovered by polling {@code api/transactions/{id}}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZeepayService {

    /** Zeepay success code on initiate — note this is the STRING "411". */
    private static final String INITIATE_SUCCESS_CODE = "411";
    /** Zeepay success code on account verification. */
    private static final String ACCOUNT_VERIFY_SUCCESS_CODE = "200";
    /** data.status value (case-insensitive) that means "paid out". */
    private static final String STATUS_SUCCESS = "Success";

    private static final String SENDER_COUNTRY = "UK";
    private static final String SENDING_CURRENCY = "GBP";
    private static final String TRANSACTION_TYPE_CREDIT = "CR";

    private static final DateTimeFormatter NONCE = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final ZeepayConfig config;
    private final ZeePayRepository zeePayRepository;
    private final TransactionRepository transactionRepository;
    private final BeneficiaryRepository beneficiaryRepository;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    // ------------------------------------------------------------------
    // Initiate
    // ------------------------------------------------------------------

    /**
     * Build the correct multipart form for the requested service type, POST it to Zeepay,
     * persist a {@link ZeePayEntity}, and (on success "411") move the transaction to
     * {@link TransactionStatus#SENT_TO_PAYOUT}.
     */
    @Transactional
    public ZeepayInitiateResponse initiatePayout(ZeepayInitiateRequest req) {
        TransactionEntity tx = transactionRepository.findByReferenceNumber(req.getReferenceNumber())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found for referenceNumber=" + req.getReferenceNumber()));

        ZeepayServiceType serviceType = req.getServiceType();
        if (serviceType == null) {
            throw new IllegalArgumentException("serviceType is required (WALLET | BANK | PICKUP)");
        }

        // Resolve sender names: request overrides win, else fall back to the sender user.
        String senderFirst = req.getSenderFirstName();
        String senderLast = req.getSenderLastName();
        if ((senderFirst == null || senderFirst.isBlank()) || (senderLast == null || senderLast.isBlank())) {
            UserEntity sender = userRepository.findById(tx.getSenderId()).orElse(null);
            if (sender != null) {
                if (senderFirst == null || senderFirst.isBlank()) senderFirst = sender.getFirstName();
                if (senderLast == null || senderLast.isBlank()) senderLast = sender.getLastName();
            }
        }
        senderFirst = nvl(senderFirst, "");
        senderLast = nvl(senderLast, "");

        String extrId = buildExtrId(tx.getReferenceNumber());
        String receiverCurrency = mapReceiverCurrency(tx.getReceiveCurrency());
        String sendAmount = plain(tx.getSendAmount());
        String amount = plain(tx.getReceiveAmount());

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        // Common fields
        form.add("sender_first_name", senderFirst);
        form.add("sender_last_name", senderLast);
        form.add("sender_country", SENDER_COUNTRY);
        form.add("receiver_first_name", nvl(req.getReceiverFirstName(), ""));
        form.add("receiver_last_name", nvl(req.getReceiverLastName(), ""));
        form.add("receiver_country", nvl(req.getReceiverCountry(), ""));
        form.add("send_amount", sendAmount);
        form.add("amount", amount);
        form.add("receiver_currency", receiverCurrency);
        form.add("extr_id", extrId);
        form.add("service_type", serviceType.getApiValue());

        switch (serviceType) {
            case WALLET -> {
                form.add("address", nvl(req.getAddress(), ""));
                form.add("receiver_msisdn", buildMsisdn(req.getDialingCode(), req.getRecipientMsisdn()));
                form.add("mno", nvl(req.getMno(), ""));
                form.add("transaction_type", TRANSACTION_TYPE_CREDIT);
            }
            case BANK -> {
                form.add("routing_number", nvl(req.getRoutingNumber(), ""));
                form.add("sending_currency", SENDING_CURRENCY);
                form.add("account_number", nvl(req.getAccountNumber(), ""));
            }
            case PICKUP -> {
                form.add("receiver_msisdn", buildMsisdn(req.getDialingCode(), req.getRecipientMsisdn()));
            }
        }

        // Persist the record up-front so we have an audit trail even if the call fails.
        ZeePayEntity record = ZeePayEntity.builder()
                .extraId(extrId)
                .transactionId(tx.getReferenceNumber())
                .serviceType(serviceType.getApiValue())
                .status("PENDING")
                .amountCharged(tx.getSendAmount())
                .amountSent(tx.getReceiveAmount())
                .senderCountry(SENDER_COUNTRY)
                .senderFirstName(senderFirst)
                .senderLastName(senderLast)
                .recipientFirstName(req.getReceiverFirstName())
                .recipientLastName(req.getReceiverLastName())
                .build();

        ZeepayInitiateResponse response = null;
        try {
            HttpEntity<MultiValueMap<String, String>> entity =
                    new HttpEntity<>(form, multipartHeaders());

            log.info("Zeepay initiate [{}] ref={} extrId={}",
                    serviceType, tx.getReferenceNumber(), extrId);

            ResponseEntity<ZeepayInitiateResponse> resp = restTemplate.exchange(
                    config.getUrl() + "api/payouts", HttpMethod.POST, entity,
                    ZeepayInitiateResponse.class);
            response = resp.getBody();

            log.info("Zeepay initiate HTTP {} body={}", resp.getStatusCode(), response);

            if (response != null) {
                record.setStatusCode(response.getCode());
                record.setStatusMessage(response.getMessage());
                record.setZeePayId(response.getZeepayId());

                if (INITIATE_SUCCESS_CODE.equals(response.getCode())) {
                    record.setStatus(TransactionStatus.SENT_TO_PAYOUT.name());
                    markTransactionSentToPayout(tx, response.getZeepayId(), extrId);
                } else {
                    record.setStatus("FAILED");
                    log.warn("Zeepay initiate non-success code={} message={}",
                            response.getCode(), response.getMessage());
                }
            } else {
                record.setStatus("FAILED");
                record.setStatusMessage("Empty response from Zeepay");
            }
        } catch (Exception ex) {
            record.setStatus("FAILED");
            record.setStatusMessage("Exception: " + ex.getMessage());
            log.error("Zeepay initiate failed for ref={}", tx.getReferenceNumber(), ex);
        } finally {
            zeePayRepository.save(record);
        }

        return response;
    }

    private void markTransactionSentToPayout(TransactionEntity tx, String zeepayId, String extrId) {
        tx.setPayoutReference(zeepayId != null ? zeepayId : extrId);
        // SENT_TO_PAYOUT maps to the legacy "sent for pay" state.
        tx.setStatus(TransactionStatus.SENT_TO_PAYOUT);
        transactionRepository.save(tx);
        log.info("Transaction {} -> SENT_TO_PAYOUT (zeepayId={})", tx.getReferenceNumber(), zeepayId);
    }

    // ------------------------------------------------------------------
    // Auto-disburse (build from beneficiary — like the old laylaremitm code)
    // ------------------------------------------------------------------

    /**
     * Auto-disburse a transaction via Zeepay, building the request from the transaction's
     * BENEFICIARY exactly like the old laylaremitm zeepay{Bank,Wallet,Pickup}Request:
     *   Bank   -> service_type=Bank,   account_number + routing_number (sort code)
     *   Wallet -> service_type=Wallet, receiver_msisdn (mobile) + mno (provider)
     *   Pickup -> service_type=Pickup, receiver_msisdn (mobile)
     * On Zeepay success code "411" {@code initiatePayout} moves the transaction to
     * SENT_TO_PAYOUT; the final PAID transition arrives via the status poll. This is what
     * the payout-partner portal's "Pay via Zeepay" button calls.
     */
    @Transactional
    public ZeepayInitiateResponse disburse(String referenceNumber) {
        log.info("========== ZEEPAY disburse (auto) START | ref={} ==========", referenceNumber);
        TransactionEntity tx = transactionRepository.findByReferenceNumber(referenceNumber).orElse(null);
        if (tx == null) {
            return failResponse("Transaction not found: " + referenceNumber);
        }
        // Zeepay corridors: Ghana (GHS), Zimbabwe (ZWL/ZWG), Zambia (ZMW), Nigeria (NGN->USD).
        String receiverCountry = zeepayCountryForCurrency(tx.getReceiveCurrency());
        if (receiverCountry.isEmpty()) {
            return failResponse("Zeepay payout not supported for currency "
                    + tx.getReceiveCurrency() + " (supported: GHS, ZWL, ZMW, NGN)");
        }
        if (tx.getBeneficiaryId() == null) {
            return failResponse("No beneficiary on transaction " + referenceNumber);
        }
        BeneficiaryEntity bene = beneficiaryRepository.findById(tx.getBeneficiaryId()).orElse(null);
        if (bene == null) {
            return failResponse("Beneficiary not found for transaction " + referenceNumber);
        }

        ZeepayInitiateRequest req = new ZeepayInitiateRequest();
        req.setReferenceNumber(referenceNumber);
        req.setReceiverCountry(receiverCountry);
        req.setAddress(bene.getAddress());
        String[] names = splitName(bene.getFullName());
        req.setReceiverFirstName(names[0]);
        req.setReceiverLastName(names[1]);

        DeliveryMethod dm = bene.getDeliveryMethod() != null ? bene.getDeliveryMethod() : tx.getDeliveryMethod();
        if (dm == DeliveryMethod.BANK_DEPOSIT) {
            req.setServiceType(ZeepayServiceType.BANK);
            req.setAccountNumber(bene.getAccountNumber());
            req.setRoutingNumber(bene.getSortCode());
        } else if (dm == DeliveryMethod.CASH_PICKUP) {
            req.setServiceType(ZeepayServiceType.PICKUP);
            // mobile_number already carries the full international MSISDN — don't re-prefix.
            req.setDialingCode("");
            req.setRecipientMsisdn(bene.getMobileNumber());
        } else {
            req.setServiceType(ZeepayServiceType.WALLET);
            req.setDialingCode("");
            req.setRecipientMsisdn(bene.getMobileNumber());
            req.setMno(bene.getMobileProvider());
        }
        log.info("ZEEPAY disburse: type={} country={} recipient={} ref={}", req.getServiceType(),
                receiverCountry, req.getRecipientMsisdn() != null ? req.getRecipientMsisdn() : req.getAccountNumber(),
                referenceNumber);

        ZeepayInitiateResponse resp = initiatePayout(req);
        log.info("========== ZEEPAY disburse (auto) END | ref={} | code={} ==========",
                referenceNumber, resp != null ? resp.getCode() : "null");
        return resp;
    }

    /** Map a receive currency to the ISO-2 receiver country Zeepay expects ("" if unsupported). */
    private String zeepayCountryForCurrency(String currency) {
        if (currency == null) return "";
        switch (currency.toUpperCase()) {
            case "GHS": return "GH";
            case "ZWL":
            case "ZWG": return "ZW";
            case "ZMW": return "ZM";
            case "NGN": return "NG";
            default: return "";
        }
    }

    /** Split a full name into [first, last]; last is "" when there's only one token. */
    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) return new String[]{"", ""};
        String trimmed = fullName.trim();
        int sp = trimmed.indexOf(' ');
        return sp < 0
                ? new String[]{trimmed, ""}
                : new String[]{trimmed.substring(0, sp), trimmed.substring(sp + 1).trim()};
    }

    /** Build a non-success ZeepayInitiateResponse carrying an error message for the caller. */
    private ZeepayInitiateResponse failResponse(String message) {
        ZeepayInitiateResponse r = new ZeepayInitiateResponse();
        r.setCode("ERR");
        r.setMessage(message);
        return r;
    }

    // ------------------------------------------------------------------
    // Status check
    // ------------------------------------------------------------------

    /**
     * Poll Zeepay for the current status of the payout behind the given internal reference.
     * On "Success" the transaction (and ZeePay record) is marked PAID.
     */
    @Transactional
    public ZeepayStatusResponse checkStatus(String referenceNumber) {
        ZeePayEntity record = zeePayRepository.findFirstByTransactionIdOrderByIdDesc(referenceNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Zeepay record for referenceNumber=" + referenceNumber));
        return pollAndApply(record);
    }

    /**
     * Calls the status endpoint for a single record (using its zee_pay_id, falling back to
     * extr_id) and applies the result. Returns the parsed response (may be null on error).
     */
    public ZeepayStatusResponse pollAndApply(ZeePayEntity record) {
        String lookupId = record.getZeePayId() != null && !record.getZeePayId().isBlank()
                ? record.getZeePayId()
                : record.getExtraId();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(jsonHeaders());
            ResponseEntity<ZeepayStatusResponse> resp = restTemplate.exchange(
                    config.getUrl() + "api/transactions/" + lookupId,
                    HttpMethod.GET, entity, ZeepayStatusResponse.class);
            ZeepayStatusResponse body = resp.getBody();

            log.info("Zeepay status HTTP {} id={} body={}", resp.getStatusCode(), lookupId, body);

            if (body != null && STATUS_SUCCESS.equalsIgnoreCase(body.getStatus())) {
                applyPaid(record);
            } else if (body != null && body.getStatus() != null) {
                record.setStatus(body.getStatus());
                zeePayRepository.save(record);
            }
            return body;
        } catch (Exception ex) {
            log.error("Zeepay status check failed for id={}", lookupId, ex);
            return null;
        }
    }

    private void applyPaid(ZeePayEntity record) {
        record.setStatus(TransactionStatus.PAID.name());
        zeePayRepository.save(record);

        Optional<TransactionEntity> txOpt = transactionRepository.findByReferenceNumber(record.getTransactionId());
        if (txOpt.isPresent()) {
            TransactionEntity tx = txOpt.get();
            if (tx.getStatus() != TransactionStatus.PAID && tx.getStatus() != TransactionStatus.COMPLETED) {
                tx.setStatus(TransactionStatus.PAID);
                tx.setPayoutConfirmedAt(LocalDateTime.now());
                transactionRepository.save(tx);
                log.info("Transaction {} -> PAID (Zeepay)", tx.getReferenceNumber());
            }
        } else {
            log.warn("Zeepay paid but transaction not found for ref={}", record.getTransactionId());
        }
    }

    // ------------------------------------------------------------------
    // Helpers: account verification & bank list
    // ------------------------------------------------------------------

    /**
     * Customer-side recipient validation (ported from laylaremitm ValidateMobileWallet /
     * ValidateBankWallet). POSTs to {@code api/payouts/account-verification} and returns the
     * RAW Zeepay JSON (which carries the resolved account name) so the add-recipient form can
     * show it. {@code serviceType} = "Wallet" (mno + mobile_number) or "Bank"
     * (routing_number + account_number + receiving_country).
     */
    public String validateRecipient(String serviceType, String mno, String mobileNumber,
                                    String routingNumber, String accountNumber, String receivingCountry) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("service_type", serviceType);
        if ("Bank".equalsIgnoreCase(serviceType)) {
            form.add("routing_number", nvl(routingNumber, ""));
            form.add("account_number", nvl(accountNumber, ""));
            form.add("receiving_country", nvl(receivingCountry, ""));
        } else {
            form.add("mno", nvl(mno, ""));
            form.add("mobile_number", nvl(mobileNumber, ""));
        }
        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, multipartHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(
                    config.getUrl() + "api/payouts/account-verification",
                    HttpMethod.POST, entity, String.class);
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Zeepay recipient validation failed (serviceType={})", serviceType, ex);
            return null;
        }
    }

    /** Optional helper: verify a bank account. Returns true when Zeepay replies with code "200". */
    public boolean verifyAccount(MultiValueMap<String, String> fields) {
        try {
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(fields, multipartHeaders());
            ResponseEntity<ZeepayInitiateResponse> resp = restTemplate.exchange(
                    config.getUrl() + "api/payouts/account-verification",
                    HttpMethod.POST, entity, ZeepayInitiateResponse.class);
            ZeepayInitiateResponse body = resp.getBody();
            return body != null && ACCOUNT_VERIFY_SUCCESS_CODE.equals(body.getCode());
        } catch (Exception ex) {
            log.error("Zeepay account verification failed", ex);
            return false;
        }
    }

    /** Fetch the list of banks supported for a country ISO. Returns raw JSON, or null on error. */
    public String listBanks(String countryIso) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(jsonHeaders());
            ResponseEntity<String> resp = restTemplate.exchange(
                    config.getUrl() + "api/payouts/banks/" + countryIso,
                    HttpMethod.GET, entity, String.class);
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Zeepay bank list failed for iso={}", countryIso, ex);
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private HttpHeaders multipartHeaders() {
        HttpHeaders headers = bearer();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = bearer();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders bearer() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(config.getToken());
        return headers;
    }

    /** Unique idempotency key: timestamp nonce (yyMMddHHmmss) + transaction reference number. */
    private String buildExtrId(String referenceNumber) {
        return LocalDateTime.now().format(NONCE) + referenceNumber;
    }

    /** Currency rule: NGN is sent to Zeepay as USD; everything else passes through. */
    private String mapReceiverCurrency(String receiveCurrency) {
        return "NGN".equalsIgnoreCase(receiveCurrency) ? "USD" : receiveCurrency;
    }

    private String buildMsisdn(String dialingCode, String number) {
        return nvl(dialingCode, "") + nvl(number, "");
    }

    private String plain(BigDecimal value) {
        return value != null ? value.toPlainString() : "0";
    }

    private String nvl(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
