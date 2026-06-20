package com.remitm.modules.user.controller;

import com.remitm.common.dto.ApiResponse;
import com.remitm.modules.user.dto.AccountDeletionRequest;
import com.remitm.modules.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Customer account lifecycle endpoints (Google Play Account Deletion policy).
 */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Account lifecycle (deletion requests)")
public class AccountController {

    private final AccountService accountService;

    @Operation(summary = "Request account deletion",
            description = "Marks the authenticated user's account for deletion, disables access, "
                    + "revokes sessions, audits the event, and emails a confirmation. "
                    + "Records are retained for the legally required AML/KYC/tax period.")
    @PostMapping("/delete-request")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> requestDeletion(
            @Valid @RequestBody(required = false) AccountDeletionRequest body,
            HttpServletRequest request) {

        String token = extractBearerToken(request);
        String reason = body != null ? body.getReason() : null;

        accountService.requestAccountDeletion(token, reason, clientIp(request),
                request.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Account deletion request received. Your account has been deactivated and "
                        + "a confirmation email has been sent.")
                .build());
    }

    @Operation(summary = "Public: request a deletion verification code",
            description = "Unauthenticated. Emails a one-time code to the address if an eligible "
                    + "account exists. Always returns success (does not reveal whether the email exists).")
    @PostMapping("/public/delete-request")
    public ResponseEntity<ApiResponse<Void>> publicDeleteRequest(
            @RequestBody java.util.Map<String, String> body) {
        accountService.requestPublicDeletionOtp(body != null ? body.get("email") : null);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("If an account exists for that email, we've sent a 6-digit verification code to it.")
                .build());
    }

    @Operation(summary = "Public: confirm account deletion with the emailed code",
            description = "Unauthenticated. Verifies the OTP and soft-deletes the account "
                    + "(records retained for the legal AML/KYC/tax period).")
    @PostMapping("/public/delete-confirm")
    public ResponseEntity<ApiResponse<Void>> publicDeleteConfirm(
            @RequestBody java.util.Map<String, String> body,
            HttpServletRequest request) {
        String email = body != null ? body.get("email") : null;
        String otp = body != null ? body.get("otp") : null;
        String reason = body != null ? body.get("reason") : null;
        accountService.confirmPublicDeletion(email, otp, reason, clientIp(request),
                request.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Your account deletion request has been confirmed and your account deactivated.")
                .build());
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
