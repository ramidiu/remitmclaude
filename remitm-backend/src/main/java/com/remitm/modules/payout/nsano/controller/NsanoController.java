package com.remitm.modules.payout.nsano.controller;

import com.remitm.modules.payout.nsano.dto.NsanoApiResponse;
import com.remitm.modules.payout.nsano.dto.NsanoInitiateRequest;
import com.remitm.modules.payout.nsano.dto.NsanoStatusRequest;
import com.remitm.modules.payout.nsano.service.NsanoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-facing NSANO payout endpoints. Authenticated (see SecurityConfig:
 * /api/payout/nsano/** is authenticated; the public callback lives on /nsano).
 */
@RestController
@RequestMapping("/api/payout/nsano")
@RequiredArgsConstructor
@Slf4j
public class NsanoController {

    private final NsanoService nsanoService;

    /**
     * Customer-side recipient name lookup (ported from laylaremitm
     * findNameBasedOnWallet / findNameBasedOnAccountAndBank). Resolves the account holder's
     * name from NSANO so the add-recipient form can confirm the recipient before saving.
     *   type=wallet  → destinationHouse = mobile network (MTN/VODAFONE/AIRTELTIGO), accountNumber = mobile number
     *   type=account → destinationHouse = bank code / SWIFT,                          accountNumber = account number
     */
    @GetMapping("/name-check")
    public ResponseEntity<Map<String, Object>> nameCheck(
            @RequestParam(defaultValue = "wallet") String type,
            @RequestParam String destinationHouse,
            @RequestParam String accountNumber) {
        boolean wallet = !"account".equalsIgnoreCase(type);
        String name = nsanoService.nameCheck(wallet, accountNumber, destinationHouse);
        return ResponseEntity.ok(Map.of(
                "found", name != null && !name.isBlank(),
                "accountName", name != null ? name : ""));
    }

    /** Admin-triggered payout. */
    @PostMapping("/initiate")
    public ResponseEntity<NsanoApiResponse> initiate(@RequestBody NsanoInitiateRequest request) {
        try {
            return ResponseEntity.ok(nsanoService.initiate(request));
        } catch (Exception ex) {
            log.error("NSANO controller: initiate failed", ex);
            NsanoApiResponse err = new NsanoApiResponse();
            err.setCode("99");
            err.setMsg("Internal error: " + ex.getMessage());
            return ResponseEntity.ok(err);
        }
    }

    /**
     * Auto-disburse a transaction by reference — builds the NSANO request from the
     * transaction's beneficiary (Bank/Wallet) like the old website and marks PAID on "00".
     * Used by the payout-partner portal's "Pay via Nsano" action.
     */
    @PostMapping("/disburse/{referenceNumber}")
    public ResponseEntity<NsanoApiResponse> disburse(@PathVariable String referenceNumber) {
        try {
            return ResponseEntity.ok(nsanoService.disburse(referenceNumber));
        } catch (Exception ex) {
            log.error("NSANO controller: disburse failed | ref={}", referenceNumber, ex);
            NsanoApiResponse err = new NsanoApiResponse();
            err.setCode("99");
            err.setMsg("Internal error: " + ex.getMessage());
            return ResponseEntity.ok(err);
        }
    }

    /** Current persisted status for a transaction reference. */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestBody NsanoStatusRequest request) {
        try {
            return ResponseEntity.ok(nsanoService.currentStatus(request.getReferenceNumber()));
        } catch (Exception ex) {
            log.error("NSANO controller: status failed", ex);
            return ResponseEntity.ok(Map.of("found", false, "message", "Internal error: " + ex.getMessage()));
        }
    }
}
