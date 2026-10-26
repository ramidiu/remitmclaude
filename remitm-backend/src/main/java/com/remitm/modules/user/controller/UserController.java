package com.remitm.modules.user.controller;

import com.remitm.common.dto.ApiResponse;
import com.remitm.common.dto.EmailChangeRequest;
import com.remitm.common.dto.EmailChangeVerifyRequest;
import com.remitm.common.dto.UserResponse;
import com.remitm.common.dto.UserUpdateRequest;
import com.remitm.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user profile management")
public class UserController {

    private final UserService userService;

    /**
     * Ownership guard: allow only the account owner (JWT subject == uuid) OR a staff member.
     * Prevents any authenticated user from reading/editing another user's profile via the {uuid}
     * path variable (IDOR).
     *
     * Staff is detected by an admin/compliance-only authority ({@code user:suspend} or
     * {@code user:approve_kyc}). NOTE: {@code user:view}/{@code user:edit} are intentionally NOT
     * used here — the CUSTOMER role holds those for self-service, so using them as the staff marker
     * would let any customer read every other user's profile.
     */
    private void assertSelfOrStaff(String uuid) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "user:suspend".equals(a.getAuthority()) || "user:approve_kyc".equals(a.getAuthority()))) {
            return; // staff override (ADMIN / SUPER_ADMIN / COMPLIANCE_OFFICER)
        }
        String caller = auth != null ? auth.getName() : null;
        if (caller == null || !caller.equals(uuid)) {
            throw new com.remitm.common.exception.RemitmException(
                    "Access denied", org.springframework.http.HttpStatus.FORBIDDEN);
        }
    }

    @Operation(summary = "Get user by UUID", description = "Retrieve a user profile by their UUID")
    @GetMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @Parameter(description = "User UUID") @PathVariable String uuid) {
        assertSelfOrStaff(uuid);
        UserResponse user = userService.getUserByUuid(uuid);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(user)
                .message("User retrieved successfully")
                .build());
    }

    @Operation(summary = "Update user profile", description = "Update a user's profile information")
    @PutMapping("/{uuid}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "User UUID") @PathVariable String uuid,
            @Valid @RequestBody UserUpdateRequest request) {
        assertSelfOrStaff(uuid);
        UserResponse user = userService.updateUser(uuid, request);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(user)
                .message("User updated successfully")
                .build());
    }

    @Operation(summary = "Request email change", description = "Send a verification code to the customer's NEW email address")
    @PostMapping("/{uuid}/email-change/request")
    public ResponseEntity<ApiResponse<Void>> requestEmailChange(
            @Parameter(description = "User UUID") @PathVariable String uuid,
            @Valid @RequestBody EmailChangeRequest request) {
        assertSelfOrStaff(uuid);
        userService.requestEmailChange(uuid, request.getNewEmail());
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Verification code sent to your new email address")
                .build());
    }

    @Operation(summary = "Verify email change", description = "Verify the OTP sent to the new email and apply the change")
    @PostMapping("/{uuid}/email-change/verify")
    public ResponseEntity<ApiResponse<UserResponse>> verifyEmailChange(
            @Parameter(description = "User UUID") @PathVariable String uuid,
            @Valid @RequestBody EmailChangeVerifyRequest request) {
        assertSelfOrStaff(uuid);
        UserResponse user = userService.verifyEmailChange(uuid, request.getNewEmail(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(user)
                .message("Email address updated successfully")
                .build());
    }

    @Operation(summary = "List users", description = "List all users with optional filtering and pagination (admin only)")
    @GetMapping
    @PreAuthorize("hasPermission(null, 'user:view')")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Search term (name or email)") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by KYC tier") @RequestParam(required = false) String kycTier,
            @Parameter(description = "Filter by KYC status: VERIFIED, REJECTED, PENDING, PARTIAL") @RequestParam(required = false) String kycStatus,
            @Parameter(description = "Sort: alpha (A-Z by name), recent (last updated), default = createdAt desc") @RequestParam(required = false) String sort) {
        Page<UserResponse> users = userService.listUsers(page, size, search, status, kycTier, kycStatus, sort);
        return ResponseEntity.ok(ApiResponse.<Page<UserResponse>>builder()
                .success(true)
                .data(users)
                .message("Users retrieved successfully")
                .build());
    }

    @Operation(summary = "Suspend user", description = "Suspend a user account (admin only)")
    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasPermission(null, 'user:suspend')")
    public ResponseEntity<ApiResponse<UserResponse>> suspendUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "Suspension reason") @RequestParam(required = false) String reason) {
        UserResponse user = userService.suspendUser(id, reason);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(user)
                .message("User suspended successfully")
                .build());
    }

    @Operation(summary = "Activate user", description = "Activate a suspended user account (admin only)")
    @PutMapping("/{id}/activate")
    @PreAuthorize("hasPermission(null, 'user:suspend')")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        UserResponse user = userService.activateUser(id);
        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .data(user)
                .message("User activated successfully")
                .build());
    }
}
