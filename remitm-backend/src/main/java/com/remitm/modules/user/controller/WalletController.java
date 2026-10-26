package com.remitm.modules.user.controller;

import com.remitm.common.dto.ApiResponse;
import com.remitm.common.exception.ResourceNotFoundException;
import com.remitm.modules.user.entity.WalletEntity;
import com.remitm.modules.user.entity.WalletLedgerEntity;
import com.remitm.modules.auth.repository.UserRepository;
import com.remitm.modules.user.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    // ── Customer Endpoints ──────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<WalletEntity>> getMyWallet(
            @RequestHeader("X-User-UUID") String uuid) {
        Long userId = resolveUserId(uuid);
        WalletEntity wallet = walletService.getOrCreateWallet(userId);
        return ResponseEntity.ok(ApiResponse.<WalletEntity>builder()
                .success(true)
                .data(wallet)
                .message("Wallet retrieved successfully")
                .build());
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletLedgerEntity>>> getMyTransactions(
            @RequestHeader("X-User-UUID") String uuid) {
        Long userId = resolveUserId(uuid);
        List<WalletLedgerEntity> transactions = walletService.getTransactions(userId);
        return ResponseEntity.ok(ApiResponse.<List<WalletLedgerEntity>>builder()
                .success(true)
                .data(transactions)
                .message("Transactions retrieved successfully")
                .build());
    }

    @PostMapping("/debit")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<WalletEntity>> debit(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        BigDecimal amount = requirePositiveAmount(body);
        String description = body.getOrDefault("description", "").toString();
        String referenceId = body.getOrDefault("referenceId", "").toString();
        String referenceType = body.getOrDefault("referenceType", "").toString();

        WalletEntity wallet = walletService.debit(userId, amount, description, referenceId, referenceType);
        return ResponseEntity.ok(ApiResponse.<WalletEntity>builder()
                .success(true)
                .data(wallet)
                .message("Wallet debited successfully")
                .build());
    }

    @PostMapping("/credit")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<WalletEntity>> credit(@RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        BigDecimal amount = requirePositiveAmount(body);
        String description = body.getOrDefault("description", "").toString();
        String referenceId = body.getOrDefault("referenceId", "").toString();
        String referenceType = body.getOrDefault("referenceType", "").toString();

        WalletEntity wallet = walletService.credit(userId, amount, description, referenceId, referenceType);
        return ResponseEntity.ok(ApiResponse.<WalletEntity>builder()
                .success(true)
                .data(wallet)
                .message("Wallet credited successfully")
                .build());
    }

    // ── Admin Endpoints ─────────────────────────────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<List<WalletEntity>>> getAllWallets() {
        List<WalletEntity> wallets = walletService.getAllWallets();
        return ResponseEntity.ok(ApiResponse.<List<WalletEntity>>builder()
                .success(true)
                .data(wallets)
                .message("All wallets retrieved successfully")
                .build());
    }

    @GetMapping("/admin/{userId}/transactions")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<List<WalletLedgerEntity>>> getAdminTransactions(
            @PathVariable Long userId) {
        List<WalletLedgerEntity> transactions = walletService.getTransactions(userId);
        return ResponseEntity.ok(ApiResponse.<List<WalletLedgerEntity>>builder()
                .success(true)
                .data(transactions)
                .message("Transactions retrieved successfully")
                .build());
    }

    @PostMapping("/admin/{userId}/credit")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<WalletEntity>> adminCredit(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = requirePositiveAmount(body);
        String description = body.getOrDefault("description", "Admin credit").toString();

        WalletEntity wallet = walletService.adminCredit(userId, amount, description);
        return ResponseEntity.ok(ApiResponse.<WalletEntity>builder()
                .success(true)
                .data(wallet)
                .message("Admin credit applied successfully")
                .build());
    }

    @PostMapping("/admin/{userId}/debit")
    @PreAuthorize("hasPermission(null, 'wallet:manage')")
    public ResponseEntity<ApiResponse<WalletEntity>> adminDebit(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = requirePositiveAmount(body);
        String description = body.getOrDefault("description", "Admin debit").toString();

        WalletEntity wallet = walletService.adminDebit(userId, amount, description);
        return ResponseEntity.ok(ApiResponse.<WalletEntity>builder()
                .success(true)
                .data(wallet)
                .message("Admin debit applied successfully")
                .build());
    }

    // ── Helper ──────────────────────────────────────────────────────────

    /**
     * Validate and parse the {@code amount} field from a request body. Rejects a missing,
     * non-numeric, zero or negative amount with HTTP 400 instead of letting a NumberFormatException
     * or a negative-value ledger movement slip through.
     */
    private BigDecimal requirePositiveAmount(Map<String, Object> body) {
        Object raw = body.get("amount");
        if (raw == null) {
            throw new com.remitm.common.exception.RemitmException(
                    "amount is required", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw.toString());
        } catch (NumberFormatException e) {
            throw new com.remitm.common.exception.RemitmException(
                    "amount is not a valid number", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (amount.signum() <= 0) {
            throw new com.remitm.common.exception.RemitmException(
                    "amount must be greater than zero", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        return amount;
    }

    private Long resolveUserId(String uuid) {
        return userRepository.findByUuid(uuid)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", uuid));
    }
}
