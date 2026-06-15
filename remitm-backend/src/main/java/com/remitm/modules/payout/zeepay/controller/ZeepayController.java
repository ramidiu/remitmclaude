package com.remitm.modules.payout.zeepay.controller;

import com.remitm.modules.payout.zeepay.dto.ZeepayInitiateRequest;
import com.remitm.modules.payout.zeepay.dto.ZeepayInitiateResponse;
import com.remitm.modules.payout.zeepay.dto.ZeepayStatusResponse;
import com.remitm.modules.payout.zeepay.service.ZeepayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin/back-office endpoints to drive Zeepay payouts. Secured by the global
 * {@code anyRequest().authenticated()} rule (no public Zeepay endpoint exists).
 */
@RestController
@RequestMapping("/api/payout/zeepay")
@RequiredArgsConstructor
@Slf4j
public class ZeepayController {

    private final ZeepayService zeepayService;

    /** Initiate a wallet / bank / pickup disbursement for an existing transaction. */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(@RequestBody ZeepayInitiateRequest request) {
        try {
            ZeepayInitiateResponse response = zeepayService.initiatePayout(request);
            if (response == null) {
                return ResponseEntity.status(502).body(Map.of(
                        "success", false,
                        "message", "No response from Zeepay"));
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Zeepay initiate bad request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Zeepay initiate error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Auto-disburse a transaction by reference — builds the Zeepay request from the
     * transaction's beneficiary (Bank/Wallet/Pickup) like the old website. On code "411"
     * the transaction moves to SENT_TO_PAYOUT (final PAID arrives via the status poll).
     * Used by the payout-partner portal's "Pay via Zeepay" action.
     */
    @PostMapping("/disburse/{referenceNumber}")
    public ResponseEntity<?> disburse(@PathVariable String referenceNumber) {
        try {
            ZeepayInitiateResponse response = zeepayService.disburse(referenceNumber);
            if (response == null) {
                return ResponseEntity.status(502).body(Map.of(
                        "success", false, "message", "No response from Zeepay"));
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Zeepay disburse error | ref={}", referenceNumber, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Poll the latest Zeepay status for the given internal transaction reference. */
    @PostMapping("/status")
    public ResponseEntity<?> status(@RequestBody Map<String, String> body) {
        String referenceNumber = body.get("referenceNumber");
        if (referenceNumber == null || referenceNumber.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "referenceNumber is required"));
        }
        try {
            ZeepayStatusResponse response = zeepayService.checkStatus(referenceNumber);
            if (response == null) {
                return ResponseEntity.status(502).body(Map.of(
                        "success", false,
                        "message", "No response from Zeepay"));
            }
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Zeepay status error", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Passthrough: list banks supported by Zeepay for a country ISO. */
    @GetMapping("/banks/{iso}")
    public ResponseEntity<?> banks(@PathVariable("iso") String iso) {
        String body = zeepayService.listBanks(iso);
        if (body == null) {
            return ResponseEntity.status(502).body(Map.of("success", false, "message", "No response from Zeepay"));
        }
        return ResponseEntity.ok(body);
    }

    /**
     * Customer-side recipient validation (ported from laylaremitm ValidateMobileWallet).
     * mno + mobileNumber → returns the raw Zeepay account-verification JSON (carries the name).
     */
    @GetMapping("/validate/wallet")
    public ResponseEntity<?> validateWallet(@RequestParam String mno, @RequestParam String mobileNumber) {
        String body = zeepayService.validateRecipient("Wallet", mno, mobileNumber, null, null, null);
        if (body == null) {
            return ResponseEntity.status(502).body(Map.of("success", false, "message", "No response from Zeepay"));
        }
        return ResponseEntity.ok(body);
    }

    /**
     * Customer-side recipient validation (ported from laylaremitm ValidateBankWallet).
     * routingNumber + accountNumber + receivingCountry → raw Zeepay account-verification JSON.
     */
    @GetMapping("/validate/bank")
    public ResponseEntity<?> validateBank(@RequestParam String routingNumber,
                                          @RequestParam String accountNumber,
                                          @RequestParam String receivingCountry) {
        String body = zeepayService.validateRecipient("Bank", null, null, routingNumber, accountNumber, receivingCountry);
        if (body == null) {
            return ResponseEntity.status(502).body(Map.of("success", false, "message", "No response from Zeepay"));
        }
        return ResponseEntity.ok(body);
    }
}
