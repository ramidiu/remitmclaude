package com.remitm.modules.user.service;

import com.remitm.common.dto.UserResponse;
import com.remitm.common.dto.UserUpdateRequest;
import com.remitm.common.enums.KycTier;
import com.remitm.common.enums.UserStatus;
import com.remitm.common.exception.RemitmException;
import com.remitm.common.exception.ResourceNotFoundException;
import com.remitm.modules.auth.entity.UserEntity;
import com.remitm.modules.auth.repository.UserRepository;
import com.remitm.modules.auth.service.AuthEmailService;
import com.remitm.modules.auth.service.OtpService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final AuthEmailService authEmailService;

    /** Namespace the email-change OTP so it can't collide with the login/registration OTP. */
    private String emailChangeOtpKey(String email) {
        return "emailchg:" + email;
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUuid(String uuid) {
        UserEntity user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", uuid));
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(String uuid, UserUpdateRequest request) {
        UserEntity user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", uuid));

        // Track if sensitive fields changed (requires re-verification)
        boolean sensitiveChanged = false;
        // Track address change specifically — it forces a re-upload of proof-of-address + identity.
        boolean addressChanged = false;

        if (StringUtils.hasText(request.getFirstName()) && !request.getFirstName().equals(user.getFirstName())) {
            user.setFirstName(request.getFirstName());
            sensitiveChanged = true;
        }
        if (StringUtils.hasText(request.getLastName()) && !request.getLastName().equals(user.getLastName())) {
            user.setLastName(request.getLastName());
            sensitiveChanged = true;
        }
        if (StringUtils.hasText(request.getPhone()) && !request.getPhone().equals(user.getPhone())) {
            user.setPhone(request.getPhone());
            sensitiveChanged = true;
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (StringUtils.hasText(request.getCountry())) {
            user.setCountry(request.getCountry());
        }
        if (StringUtils.hasText(request.getAddressLine1()) && !request.getAddressLine1().equals(user.getAddressLine1())) {
            user.setAddressLine1(request.getAddressLine1());
            sensitiveChanged = true;
            addressChanged = true;
        }
        if (request.getAddressLine2() != null && !request.getAddressLine2().equals(user.getAddressLine2() == null ? "" : user.getAddressLine2())) {
            user.setAddressLine2(request.getAddressLine2());
        }
        if (StringUtils.hasText(request.getCity()) && !request.getCity().equals(user.getCity())) {
            user.setCity(request.getCity());
            sensitiveChanged = true;
            addressChanged = true;
        }
        if (StringUtils.hasText(request.getPostcode()) && !request.getPostcode().equals(user.getPostcode())) {
            user.setPostcode(request.getPostcode());
            sensitiveChanged = true;
            addressChanged = true;
        }
        if (StringUtils.hasText(request.getPreferredLanguage())) {
            user.setPreferredLanguage(request.getPreferredLanguage());
        }

        // Admin can directly set status and kycTier
        if (StringUtils.hasText(request.getStatus())) {
            try {
                user.setStatus(UserStatus.valueOf(request.getStatus().toUpperCase()));
                log.info("Admin set user {} status to {}", uuid, request.getStatus());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status: {}", request.getStatus());
            }
        }
        if (StringUtils.hasText(request.getKycTier())) {
            try {
                user.setKycTier(KycTier.valueOf(request.getKycTier().toUpperCase()));
                log.info("Admin set user {} kycTier to {}", uuid, request.getKycTier());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid kycTier: {}", request.getKycTier());
            }
        }

        // Customer self-update (no admin status/kycTier override in the request).
        boolean isCustomerSelfUpdate = !StringUtils.hasText(request.getStatus())
                && !StringUtils.hasText(request.getKycTier());

        // Any profile update requires admin review before the user can transact again
        if (request.getStatus() == null) {
            user.setStatus(UserStatus.PENDING_VERIFICATION);
            log.info("User {} profile updated — status set to PENDING_VERIFICATION pending admin review", uuid);
        }

        // ADDRESS CHANGE: force the customer to re-upload proof-of-address + identity. The flag
        // persists so the requirement is re-enforced on every login (via the frontend guard) until
        // both documents are re-submitted, at which point KycService.uploadDocument clears it.
        // Tier is dropped to TIER_0 so the customer counts as unverified until re-approved; their
        // previously APPROVED documents are preserved (KycService restores the tier on re-approval).
        if (isCustomerSelfUpdate && addressChanged) {
            user.setKycReuploadRequired(true);
            user.setKycReuploadReason("ADDRESS_CHANGED");
            user.setKycReuploadRequestedAt(LocalDateTime.now());
            user.setKycTier(KycTier.TIER_0);
            log.info("User {} changed address — re-upload of address + identity required; tier reset to TIER_0", uuid);
        }

        UserEntity saved = userRepository.save(user);
        log.info("Updated user profile: {} (sensitiveChanged={})", uuid, sensitiveChanged);
        return toUserResponse(saved);
    }

    /**
     * Email-change step 1: send a 6-digit OTP to the NEW email address. The change is only
     * applied in {@link #verifyEmailChange} after the customer proves control of that inbox.
     */
    @Transactional(readOnly = true)
    public void requestEmailChange(String uuid, String newEmailRaw) {
        UserEntity user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", uuid));

        String newEmail = newEmailRaw == null ? "" : newEmailRaw.trim();
        if (newEmail.isBlank()) {
            throw new RemitmException("New email is required", HttpStatus.BAD_REQUEST);
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new RemitmException("The new email is the same as your current email", HttpStatus.BAD_REQUEST);
        }
        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            throw new RemitmException("That email address is already in use", HttpStatus.CONFLICT);
        });

        String otp = otpService.generateOtp();
        otpService.storeOtp(emailChangeOtpKey(newEmail), otp);

        String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                + (user.getLastName() != null ? user.getLastName() : "")).trim();
        authEmailService.sendOtpEmail(newEmail, otp, fullName.isBlank() ? "Customer" : fullName);
        log.info("Email-change OTP sent to new address for user {}", uuid);
    }

    /**
     * Email-change step 2: validate the OTP sent to the new email and, if correct, apply the
     * change and mark the email verified. Availability is re-checked here in case the address
     * was taken between request and verify.
     */
    @Transactional
    public UserResponse verifyEmailChange(String uuid, String newEmailRaw, String otp) {
        UserEntity user = userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User", "uuid", uuid));

        String newEmail = newEmailRaw == null ? "" : newEmailRaw.trim();
        if (newEmail.isBlank()) {
            throw new RemitmException("New email is required", HttpStatus.BAD_REQUEST);
        }
        if (!otpService.validateOtp(emailChangeOtpKey(newEmail), otp)) {
            throw new RemitmException("Invalid or expired verification code", HttpStatus.BAD_REQUEST);
        }
        userRepository.findByEmail(newEmail).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new RemitmException("That email address is already in use", HttpStatus.CONFLICT);
            }
        });

        user.setEmail(newEmail);
        user.setEmailVerified(true);
        UserEntity saved = userRepository.save(user);
        otpService.deleteOtp(emailChangeOtpKey(newEmail));
        log.info("Email changed for user {} (emailVerified=true)", uuid);
        return toUserResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(int page, int size, String search, String status, String kycTier) {
        return listUsers(page, size, search, status, kycTier, null, null);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(int page, int size, String search, String status, String kycTier, String kycStatus) {
        return listUsers(page, size, search, status, kycTier, kycStatus, null);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(int page, int size, String search, String status, String kycTier, String kycStatus, String sort) {
        // sort:
        //   "alpha"     → firstName ASC then lastName ASC (used by Users list)
        //   "recent"    → updatedAt DESC fallback createdAt DESC (used by KYC Review)
        //   null/other  → createdAt DESC (default)
        boolean alphaSort = "alpha".equalsIgnoreCase(sort);
        Sort sortOrder;
        if (alphaSort) {
            // ORDER BY is baked into searchUsersAlpha — Pageable carries an unsorted instance.
            sortOrder = Sort.unsorted();
        } else if ("recent".equalsIgnoreCase(sort)) {
            sortOrder = Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by(Sort.Direction.DESC, "createdAt"));
        } else {
            sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        Pageable pageable = alphaSort
                ? PageRequest.of(page, size)
                : PageRequest.of(page, size, sortOrder);

        UserStatus userStatus = null;
        KycTier tier = null;
        String kycStatusUpper = null;

        if (StringUtils.hasText(status)) {
            try {
                userStatus = UserStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RemitmException("Invalid status: " + status, HttpStatus.BAD_REQUEST);
            }
        }

        if (StringUtils.hasText(kycTier)) {
            try {
                tier = KycTier.valueOf(kycTier.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RemitmException("Invalid KYC tier: " + kycTier, HttpStatus.BAD_REQUEST);
            }
        }

        if (StringUtils.hasText(kycStatus)) {
            kycStatusUpper = kycStatus.toUpperCase();
            if (!kycStatusUpper.equals("VERIFIED") && !kycStatusUpper.equals("REJECTED")
                    && !kycStatusUpper.equals("PENDING") && !kycStatusUpper.equals("PARTIAL")) {
                throw new RemitmException("Invalid kycStatus: " + kycStatus, HttpStatus.BAD_REQUEST);
            }
        }

        Page<UserEntity> users = alphaSort
                ? userRepository.searchUsersAlpha(
                        StringUtils.hasText(search) ? search : null,
                        userStatus,
                        tier,
                        kycStatusUpper,
                        pageable)
                : userRepository.searchUsers(
                        StringUtils.hasText(search) ? search : null,
                        userStatus,
                        tier,
                        kycStatusUpper,
                        pageable);

        return users.map(this::toUserResponse);
    }

    @Transactional
    public UserResponse suspendUser(Long id, String reason) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new RemitmException("User is already suspended", HttpStatus.BAD_REQUEST);
        }

        user.setStatus(UserStatus.SUSPENDED);
        UserEntity saved = userRepository.save(user);
        log.info("Suspended user: {} (id: {}), reason: {}", user.getEmail(), id, reason);
        return toUserResponse(saved);
    }

    @Transactional
    public UserResponse activateUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new RemitmException("User is already active", HttpStatus.BAD_REQUEST);
        }

        user.setStatus(UserStatus.ACTIVE);
        UserEntity saved = userRepository.save(user);
        log.info("Activated user: {} (id: {})", user.getEmail(), id);
        return toUserResponse(saved);
    }

    private UserResponse toUserResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .uuid(user.getUuid())
                .email(user.getEmail())
                .phone(user.getPhone())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .dateOfBirth(user.getDateOfBirth())
                .userType(user.getUserType())
                .kycTier(user.getKycTier())
                .status(user.getStatus())
                .accountStatus(user.getAccountStatus())
                .deleteRequestedAt(user.getDeleteRequestedAt())
                .country(user.getCountry())
                .addressLine1(user.getAddressLine1())
                .addressLine2(user.getAddressLine2())
                .city(user.getCity())
                .postcode(user.getPostcode())
                .preferredLanguage(user.getPreferredLanguage())
                .kycReuploadRequired(Boolean.TRUE.equals(user.getKycReuploadRequired()))
                .kycReuploadReason(user.getKycReuploadReason())
                .mfaEnabled(user.getMfaEnabled())
                .roles(user.getRoles() == null ? java.util.List.of()
                        : user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
