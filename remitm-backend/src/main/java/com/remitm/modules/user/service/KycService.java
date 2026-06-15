package com.remitm.modules.user.service;

import com.remitm.common.dto.KycDocumentResponse;
import com.remitm.common.dto.KycStatusResponse;
import com.remitm.common.dto.ScreeningResponse;
import com.remitm.common.enums.*;
import com.remitm.common.exception.RemitmException;
import com.remitm.common.exception.ResourceNotFoundException;
import com.remitm.modules.user.dto.ScreeningResult;
import com.remitm.modules.user.dto.VerificationResult;
import com.remitm.modules.user.config.RedisPublisher;
import com.remitm.modules.auth.entity.UserEntity;
import com.remitm.modules.auth.repository.UserRepository;
import com.remitm.modules.user.entity.*;
import com.remitm.modules.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycService {

    private final KycDocumentRepository kycDocumentRepository;
    private final KycVerificationRepository kycVerificationRepository;
    private final KycAuditLogRepository kycAuditLogRepository;
    private final UserRepository userRepository;
    private final KycVerificationProvider kycVerificationProvider;
    private final KycTierEvaluator kycTierEvaluator;
    private final RedisPublisher redisPublisher;

    @Value("${app.kyc.upload-dir}")
    private String uploadDir;

    @Value("${app.kyc.max-file-size-mb}")
    private int maxFileSizeMb;

    @Value("${app.kyc.allowed-extensions}")
    private String allowedExtensions;

    @Transactional
    public KycDocumentResponse uploadDocument(Long userId, KycDocumentType type,
                                               String documentNumber, MultipartFile file,
                                               String ipAddress, java.time.LocalDate issueDate,
                                               java.time.LocalDate expiryDate,
                                               boolean autoApprove) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        validateFile(file);

        // OVERRIDE-ON-UPLOAD: a re-upload of this document type replaces the previous
        // PENDING submission of the same type (deletes the old row + its file), so only the
        // LATEST document ever awaits admin review. Approved/Rejected history is left intact —
        // superseding a verified copy belongs to the separate re-verification flow.
        supersedePreviousPending(userId, type, ipAddress);

        String fileHash = calculateSha256(file);
        String savedFilePath = saveFile(userId, type, file);

        // When an admin uploads on the user's behalf, approve immediately so the user is
        // verified + activated in one step (no separate review step needed).
        KycDocumentEntity document = KycDocumentEntity.builder()
                .userId(userId)
                .documentType(type)
                .documentNumber(documentNumber)
                .filePath(savedFilePath)
                .fileHash(fileHash)
                .status(autoApprove ? KycDocumentStatus.APPROVED : KycDocumentStatus.PENDING)
                .verifiedAt(autoApprove ? LocalDateTime.now() : null)
                .issueDate(issueDate)
                .expiryDate(expiryDate)
                .build();

        KycDocumentEntity saved = kycDocumentRepository.save(document);
        log.info("KYC document uploaded: userId={}, type={}, docId={}, autoApprove={}",
                userId, type, saved.getId(), autoApprove);

        KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                .userId(userId)
                .action(autoApprove ? "DOCUMENT_UPLOADED_AND_APPROVED" : "DOCUMENT_UPLOADED")
                .actorId(userId)
                .details(String.format("{\"documentId\": %d, \"documentType\": \"%s\", \"fileHash\": \"%s\", \"autoApprove\": %s}",
                        saved.getId(), type, fileHash, autoApprove))
                .ipAddress(ipAddress)
                .build();
        kycAuditLogRepository.save(auditLog);

        // Admin-approved upload: upgrade tier + activate the account, then notify the user.
        if (autoApprove) {
            kycTierEvaluator.evaluateAndUpgrade(userId);
            triggerRescreenOnIdentityApproval(saved);
            try {
                Map<String, String> vars = new HashMap<>();
                vars.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Customer");
                vars.put("documentType", type != null ? type.toString().replace("_", " ") : "Document");
                redisPublisher.publishKycEvent("KYC_DOCUMENT_APPROVED",
                        user.getId(), user.getEmail(), user.getFirstName(), vars);
            } catch (Exception e) {
                log.warn("Failed to publish KYC auto-approval event: {}", e.getMessage());
            }
        } else if (user.getKycTier() != null && user.getKycTier() != KycTier.TIER_0) {
            // VERIFIED RE-UPLOAD: a currently-verified customer (TIER_1+) who submits a new
            // document is moved to UNVERIFIED (pending review) immediately. Their previously
            // APPROVED documents are PRESERVED (kept for admin reference + automatic restore);
            // only the verified TIER is suspended here. evaluateAndUpgrade() restores the tier
            // from those preserved documents when the new one is APPROVED, or when it's
            // REJECTED (see reviewDocument).
            KycTier previousTier = user.getKycTier();
            user.setKycTier(KycTier.TIER_0);
            userRepository.save(user);

            KycAuditLogEntity downgradeLog = KycAuditLogEntity.builder()
                    .userId(userId)
                    .action("KYC_MOVED_TO_PENDING_REVIEW")
                    .actorId(userId)
                    .details(String.format(
                            "{\"previousTier\": \"%s\", \"documentId\": %d, \"trigger\": \"verified customer re-upload\"}",
                            previousTier, saved.getId()))
                    .ipAddress(ipAddress)
                    .build();
            kycAuditLogRepository.save(downgradeLog);
            log.info("KYC: verified user {} moved to PENDING_REVIEW (was {}) after re-upload docId={}",
                    userId, previousTier, saved.getId());
        }

        return toDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<KycDocumentResponse> getDocuments(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return toResponsesWithLatest(kycDocumentRepository.findByUserId(userId));
    }

    /**
     * Delete a user's own PENDING document. Used by the frontend to ROLL BACK a partial
     * submission: if one document in a multi-document KYC submit succeeds but a later one
     * fails, the client deletes the already-saved ones so the user is never left in a
     * half-submitted state (e.g. identity saved but proof-of-address missing).
     * Only PENDING documents owned by the user may be deleted — never APPROVED/REJECTED ones.
     */
    @Transactional
    public void deletePendingDocument(Long userId, Long documentId) {
        KycDocumentEntity document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Document", "id", documentId));

        if (!document.getUserId().equals(userId)) {
            throw new RemitmException("Document does not belong to this user", HttpStatus.FORBIDDEN);
        }
        if (document.getStatus() != KycDocumentStatus.PENDING) {
            throw new RemitmException("Only PENDING documents can be deleted", HttpStatus.BAD_REQUEST);
        }

        // Best-effort removal of the stored file; an orphaned file is harmless if this fails.
        try {
            if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
                java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(document.getFilePath()));
            }
        } catch (Exception e) {
            log.warn("Failed to delete KYC file for docId={}: {}", documentId, e.getMessage());
        }

        kycDocumentRepository.delete(document);

        KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                .userId(userId)
                .action("DOCUMENT_DELETED")
                .actorId(userId)
                .details(String.format("{\"documentId\": %d, \"reason\": \"submission rollback\"}", documentId))
                .build();
        kycAuditLogRepository.save(auditLog);

        log.info("KYC document deleted (rollback): userId={}, docId={}", userId, documentId);
    }

    /**
     * Override-on-upload helper: remove any existing PENDING document of the SAME type for
     * this user (plus its stored file) so a re-upload REPLACES rather than stacks on top of
     * the previous pending submission. Only PENDING docs are touched — Approved/Rejected
     * documents are preserved as history. Best-effort file deletion (an orphan is harmless).
     */
    private void supersedePreviousPending(Long userId, KycDocumentType type, String ipAddress) {
        List<KycDocumentEntity> priorPending = kycDocumentRepository
                .findByUserIdAndStatus(userId, KycDocumentStatus.PENDING).stream()
                .filter(d -> d.getDocumentType() == type)
                .collect(Collectors.toList());

        for (KycDocumentEntity old : priorPending) {
            try {
                if (old.getFilePath() != null && !old.getFilePath().isBlank()) {
                    java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(old.getFilePath()));
                }
            } catch (Exception e) {
                log.warn("Failed to delete superseded KYC file for docId={}: {}", old.getId(), e.getMessage());
            }
            kycDocumentRepository.delete(old);

            KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                    .userId(userId)
                    .action("DOCUMENT_SUPERSEDED")
                    .actorId(userId)
                    .details(String.format(
                            "{\"supersededDocumentId\": %d, \"documentType\": \"%s\", \"reason\": \"replaced by newer upload\"}",
                            old.getId(), type))
                    .ipAddress(ipAddress)
                    .build();
            kycAuditLogRepository.save(auditLog);

            log.info("KYC document superseded (override-on-upload): userId={}, oldDocId={}, type={}",
                    userId, old.getId(), type);
        }
    }

    @Transactional
    public KycDocumentResponse reviewDocument(Long documentId, KycDocumentStatus status,
                                               String rejectionReason, Long reviewerId,
                                               String ipAddress) {
        KycDocumentEntity document = kycDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC Document", "id", documentId));

        if (document.getStatus() != KycDocumentStatus.PENDING) {
            throw new RemitmException(
                    "Document is not in PENDING status, current status: " + document.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        if (status == KycDocumentStatus.REJECTED && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new RemitmException("Rejection reason is required when rejecting a document",
                    HttpStatus.BAD_REQUEST);
        }

        // VERIFIED RE-UPLOAD REJECTION: if a previously APPROVED document of the SAME type was
        // preserved (i.e. a verified customer re-uploaded and is now pending review), a rejection
        // must DISCARD the new upload and RESTORE the previous verified document + tier — rather
        // than leaving a REJECTED row that strands the customer as unverified.
        if (status == KycDocumentStatus.REJECTED) {
            boolean hasPreservedApproved = kycDocumentRepository
                    .findByUserIdAndStatus(document.getUserId(), KycDocumentStatus.APPROVED).stream()
                    .anyMatch(d -> d.getDocumentType() == document.getDocumentType()
                            && !d.getId().equals(documentId));
            if (hasPreservedApproved) {
                Long userId = document.getUserId();
                KycDocumentResponse discarded = toDocumentResponse(document);
                discarded.setStatus(KycDocumentStatus.REJECTED);
                discarded.setRejectionReason(rejectionReason);

                // Discard the pending upload (row + stored file).
                try {
                    if (document.getFilePath() != null && !document.getFilePath().isBlank()) {
                        java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(document.getFilePath()));
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete discarded KYC file for docId={}: {}", documentId, e.getMessage());
                }
                kycDocumentRepository.delete(document);

                // Restore the verified tier from the PRESERVED approved documents.
                kycTierEvaluator.evaluateAndUpgrade(userId);

                java.util.Map<String, Object> details = new HashMap<>();
                details.put("documentId", documentId);
                details.put("documentType", document.getDocumentType() != null ? document.getDocumentType().toString() : null);
                details.put("rejectionReason", rejectionReason);
                details.put("outcome", "pending discarded, previous verified document restored");
                kycAuditLogRepository.save(KycAuditLogEntity.builder()
                        .userId(userId)
                        .action("PENDING_REJECTED_VERIFIED_RESTORED")
                        .actorId(reviewerId)
                        .actorRole("ADMIN")
                        .details(toAuditJson(details))
                        .ipAddress(ipAddress)
                        .build());

                // Notify the customer their re-submission was rejected (they remain verified).
                try {
                    UserEntity user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        String docType = document.getDocumentType() != null
                                ? document.getDocumentType().toString().replace("_", " ") : "Document";
                        Map<String, String> vars = new HashMap<>();
                        vars.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Customer");
                        vars.put("documentType", docType);
                        vars.put("rejectionReason", rejectionReason != null ? rejectionReason : "Not specified");
                        redisPublisher.publishKycEvent("KYC_DOCUMENT_REJECTED",
                                user.getId(), user.getEmail(), user.getFirstName(), vars);
                    }
                } catch (Exception e) {
                    log.warn("Failed to publish KYC rejection event: {}", e.getMessage());
                }

                log.info("KYC reject (verified re-upload): discarded docId={} type={} for user={}; restored verified tier",
                        documentId, document.getDocumentType(), userId);
                return discarded;
            }
        }

        document.setStatus(status);
        document.setVerifiedBy(reviewerId);
        document.setVerifiedAt(LocalDateTime.now());
        if (status == KycDocumentStatus.REJECTED) {
            document.setRejectionReason(rejectionReason);
        }

        KycDocumentEntity saved = kycDocumentRepository.save(document);
        log.info("KYC document reviewed: docId={}, status={}, reviewer={}", documentId, status, reviewerId);

        java.util.Map<String, Object> reviewDetails = new HashMap<>();
        reviewDetails.put("documentId", documentId);
        reviewDetails.put("newStatus", status != null ? status.toString() : null);
        reviewDetails.put("rejectionReason", rejectionReason);
        KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                .userId(document.getUserId())
                .action("STATUS_CHANGED")
                .actorId(reviewerId)
                .actorRole("ADMIN")
                .details(toAuditJson(reviewDetails))
                .ipAddress(ipAddress)
                .build();
        kycAuditLogRepository.save(auditLog);

        if (status == KycDocumentStatus.APPROVED) {
            kycTierEvaluator.evaluateAndUpgrade(document.getUserId());
            triggerRescreenOnIdentityApproval(document);
        }

        // Publish KYC event for email notification
        try {
            UserEntity user = userRepository.findById(document.getUserId()).orElse(null);
            if (user != null) {
                String docType = document.getDocumentType() != null
                        ? document.getDocumentType().toString().replace("_", " ")
                        : "Document";
                Map<String, String> vars = new HashMap<>();
                vars.put("firstName", user.getFirstName() != null ? user.getFirstName() : "Customer");
                vars.put("documentType", docType);

                if (status == KycDocumentStatus.APPROVED) {
                    redisPublisher.publishKycEvent("KYC_DOCUMENT_APPROVED",
                            user.getId(), user.getEmail(), user.getFirstName(), vars);
                } else if (status == KycDocumentStatus.REJECTED) {
                    vars.put("rejectionReason", rejectionReason != null ? rejectionReason : "Not specified");
                    redisPublisher.publishKycEvent("KYC_DOCUMENT_REJECTED",
                            user.getId(), user.getEmail(), user.getFirstName(), vars);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to publish KYC notification event: {}", e.getMessage());
        }

        return toDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public KycStatusResponse getKycStatus(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<KycDocumentEntity> documents = kycDocumentRepository.findByUserId(userId);
        List<KycVerificationEntity> verifications = kycVerificationRepository.findByUserId(userId);

        List<KycDocumentResponse> documentResponses = toResponsesWithLatest(documents);

        List<String> verificationDescriptions = verifications.stream()
                .map(v -> String.format("%s: %s (provider: %s)",
                        v.getVerificationType(), v.getStatus(), v.getProvider()))
                .collect(Collectors.toList());

        List<String> nextTierRequirements = calculateNextTierRequirements(user.getKycTier(), documents);

        String overallStatus = computeOverallStatus(user.getKycTier(), documents);

        return KycStatusResponse.builder()
                .userId(user.getUuid())
                .currentTier(user.getKycTier())
                .overallStatus(overallStatus)
                .documents(documentResponses)
                .verifications(verificationDescriptions)
                .nextTierRequirements(nextTierRequirements)
                .build();
    }

    @Transactional
    public void triggerVerification(Long userId, VerificationType type) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        VerificationResult result;

        switch (type) {
            case IDENTITY -> {
                List<KycDocumentEntity> docs = kycDocumentRepository.findByUserId(userId);
                KycDocumentEntity idDoc = docs.stream()
                        .filter(d -> d.getDocumentType() == KycDocumentType.PASSPORT
                                || d.getDocumentType() == KycDocumentType.DRIVING_LICENCE
                                || d.getDocumentType() == KycDocumentType.NATIONAL_ID)
                        .findFirst()
                        .orElseThrow(() -> new RemitmException(
                                "No identity document found for verification", HttpStatus.BAD_REQUEST));
                result = kycVerificationProvider.verifyIdentity(userId.toString(), idDoc);
            }
            case LIVENESS -> {
                result = kycVerificationProvider.checkLiveness(userId.toString(), new byte[0]);
            }
            default -> {
                result = VerificationResult.builder()
                        .status(VerificationStatus.PENDING)
                        .providerReference("MANUAL-" + UUID.randomUUID().toString().substring(0, 8))
                        .resultData("{\"message\": \"Verification queued for processing\"}")
                        .build();
            }
        }

        KycVerificationEntity verification = KycVerificationEntity.builder()
                .userId(userId)
                .verificationType(type)
                .provider(VerificationProvider.MANUAL)
                .providerReference(result.getProviderReference())
                .status(result.getStatus())
                .resultData(result.getResultData())
                .build();

        kycVerificationRepository.save(verification);
        log.info("Verification triggered: userId={}, type={}, status={}", userId, type, result.getStatus());

        KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                .userId(userId)
                .action("VERIFICATION_INITIATED")
                .actorId(userId)
                .details(String.format("{\"verificationType\": \"%s\", \"provider\": \"MANUAL\", \"status\": \"%s\"}",
                        type, result.getStatus()))
                .build();
        kycAuditLogRepository.save(auditLog);
    }

    @Transactional
    public List<ScreeningResponse> screenPepSanctions(Long userId, String ipAddress) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String fullName = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");
        fullName = fullName.trim();

        String dateOfBirth = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
        String country = user.getCountry();

        ScreeningResult pepResult = kycVerificationProvider.screenPEP(fullName, dateOfBirth, country);
        ScreeningResult sanctionsResult = kycVerificationProvider.screenSanctions(fullName, country);

        KycVerificationEntity pepVerification = KycVerificationEntity.builder()
                .userId(userId)
                .verificationType(VerificationType.PEP_CHECK)
                .provider(VerificationProvider.MANUAL)
                .status(VerificationStatus.PENDING)
                .resultData(pepResult.getMatchDetails())
                .build();
        kycVerificationRepository.save(pepVerification);

        KycVerificationEntity sanctionsVerification = KycVerificationEntity.builder()
                .userId(userId)
                .verificationType(VerificationType.SANCTIONS_CHECK)
                .provider(VerificationProvider.MANUAL)
                .status(VerificationStatus.PENDING)
                .resultData(sanctionsResult.getMatchDetails())
                .build();
        kycVerificationRepository.save(sanctionsVerification);

        KycAuditLogEntity auditLog = KycAuditLogEntity.builder()
                .userId(userId)
                .action("SCREENING_RUN")
                .details(String.format("{\"screeningTypes\": [\"PEP_CHECK\", \"SANCTIONS_CHECK\"], \"fullName\": \"%s\"}", fullName))
                .ipAddress(ipAddress)
                .build();
        kycAuditLogRepository.save(auditLog);

        log.info("PEP/Sanctions screening completed for userId={}", userId);

        List<ScreeningResponse> responses = new ArrayList<>();
        responses.add(ScreeningResponse.builder()
                .id(pepVerification.getId())
                .entityType(EntityType.CUSTOMER)
                .entityId(userId)
                .listChecked(ScreeningListType.HMT)
                .matchScore(pepResult.getMatchScore())
                .status(pepResult.getStatus())
                .matchDetails(pepResult.getMatchDetails())
                .build());

        responses.add(ScreeningResponse.builder()
                .id(sanctionsVerification.getId())
                .entityType(EntityType.CUSTOMER)
                .entityId(userId)
                .listChecked(ScreeningListType.OFAC)
                .matchScore(sanctionsResult.getMatchScore())
                .status(sanctionsResult.getStatus())
                .matchDetails(sanctionsResult.getMatchDetails())
                .build());

        return responses;
    }

    private void triggerRescreenOnIdentityApproval(KycDocumentEntity document) {
        try {
            if (document.getDocumentType() == null) return;
            String type = document.getDocumentType().name();
            boolean isIdentityDoc = type.equals("PASSPORT")
                    || type.equals("DRIVING_LICENCE")
                    || type.equals("NATIONAL_ID");
            if (!isIdentityDoc) return;

            UserEntity user = userRepository.findById(document.getUserId()).orElse(null);
            if (user == null) return;

            String fullName = ((user.getFirstName() != null ? user.getFirstName() : "") + " "
                    + (user.getLastName() != null ? user.getLastName() : "")).trim();
            if (fullName.isBlank()) return;

            String dob = user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null;
            String country = user.getCountry();

            kycVerificationProvider.screenSanctions(fullName, country);
            kycVerificationProvider.screenPEP(fullName, dob, country);
            log.info("Re-screen dispatched for userId={} on identity doc approval", user.getId());
        } catch (Exception e) {
            log.warn("Re-screen on identity approval failed: {}", e.getMessage());
        }
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper AUDIT_JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * Serialize audit-log details to VALID, escaped JSON. The kyc_audit_log.details column is a
     * MySQL JSON type, so any free-text value (e.g. a rejection reason containing quotes,
     * newlines, emoji or pasted smart-quotes) must be properly escaped. Building this JSON by
     * hand with String.format produced malformed JSON and failed the insert with a 500.
     */
    private String toAuditJson(java.util.Map<String, Object> fields) {
        try {
            return AUDIT_JSON.writeValueAsString(fields);
        } catch (Exception e) {
            log.warn("Failed to serialize audit details: {}", e.getMessage());
            return "{}";
        }
    }

    private String computeOverallStatus(KycTier tier, List<KycDocumentEntity> documents) {
        if (documents.isEmpty()) return "NOT_SUBMITTED";

        // A pending doc only counts as a real review if it was uploaded via the app
        // (file_hash set). A genuine app submission awaiting review always wins.
        boolean anyRealPending = documents.stream().anyMatch(d ->
                d.getStatus() == KycDocumentStatus.PENDING
                        && d.getFileHash() != null && !d.getFileHash().isBlank());
        if (anyRealPending) return "PENDING";

        // Once the user has reached a verified tier (TIER_1+), leftover auto-imported
        // PENDING docs (no file_hash) are migration artifacts and must NOT drag the
        // status back to PARTIAL — the user is already verified.
        if (tier != null && tier != KycTier.TIER_0) return "VERIFIED";

        // Not yet verified: an auto-imported (no file_hash) pending doc means the
        // imported documents have not been reviewed yet = PARTIAL.
        if (documents.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.PENDING)) return "PARTIAL";
        if (documents.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.REJECTED)) return "REJECTED";

        // VERIFIED requires the COMPLETE approved set — an identity document AND a proof of address.
        // A migrated/imported customer holding only one of them is PARTIAL, so the KYC guard forces
        // them to upload the missing document(s) at login (matches "missing any → force upload").
        boolean hasIdentity = documents.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.APPROVED
                && (d.getDocumentType() == KycDocumentType.PASSPORT
                 || d.getDocumentType() == KycDocumentType.DRIVING_LICENCE
                 || d.getDocumentType() == KycDocumentType.NATIONAL_ID));
        boolean hasAddress = documents.stream().anyMatch(d -> d.getStatus() == KycDocumentStatus.APPROVED
                && d.getDocumentType() == KycDocumentType.PROOF_OF_ADDRESS);
        return (hasIdentity && hasAddress) ? "VERIFIED" : "PARTIAL";
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RemitmException("File is empty", HttpStatus.BAD_REQUEST);
        }

        long maxSizeBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new RemitmException(
                    String.format("File size exceeds maximum allowed size of %dMB", maxFileSizeMb),
                    HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RemitmException("File must have a valid extension", HttpStatus.BAD_REQUEST);
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        Set<String> allowed = Set.of(allowedExtensions.split(","));
        if (!allowed.contains(extension)) {
            throw new RemitmException(
                    String.format("File extension '%s' is not allowed. Allowed: %s", extension, allowedExtensions),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String saveFile(Long userId, KycDocumentType type, MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir, userId.toString());
            Files.createDirectories(uploadPath);

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null
                    ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                    : ".bin";
            String filename = type.name().toLowerCase() + "_" + System.currentTimeMillis() + extension;
            Path filePath = uploadPath.resolve(filename);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug("File saved: {}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save file for userId={}: {}", userId, e.getMessage());
            throw new RemitmException("Failed to save uploaded file", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private String calculateSha256(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to calculate file hash: {}", e.getMessage());
            throw new RemitmException("Failed to calculate file hash", HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private List<String> calculateNextTierRequirements(KycTier currentTier, List<KycDocumentEntity> documents) {
        Set<KycDocumentType> approvedTypes = documents.stream()
                .filter(d -> d.getStatus() == KycDocumentStatus.APPROVED)
                .map(KycDocumentEntity::getDocumentType)
                .collect(Collectors.toSet());

        List<String> requirements = new ArrayList<>();

        boolean hasIdDoc = approvedTypes.contains(KycDocumentType.PASSPORT)
                || approvedTypes.contains(KycDocumentType.DRIVING_LICENCE)
                || approvedTypes.contains(KycDocumentType.NATIONAL_ID);

        switch (currentTier) {
            case TIER_0 -> {
                if (!hasIdDoc) {
                    requirements.add("Upload and get approved: Identity document (Passport, Driving Licence, or National ID)");
                }
            }
            case TIER_1 -> {
                if (!approvedTypes.contains(KycDocumentType.PROOF_OF_ADDRESS)) {
                    requirements.add("Upload and get approved: Proof of Address document");
                }
            }
            case TIER_2 -> {
                if (!approvedTypes.contains(KycDocumentType.SOURCE_OF_FUNDS)) {
                    requirements.add("Upload and get approved: Source of Funds document");
                }
            }
            case TIER_3 -> {
                requirements.add("Maximum tier reached");
            }
        }

        return requirements;
    }

    @Transactional(readOnly = true)
    public KycDocumentEntity getDocumentEntity(Long docId) {
        return kycDocumentRepository.findById(docId).orElse(null);
    }

    /**
     * Map documents to responses and flag the newest document of each type as {@code latest}.
     * This gives the admin UI a clean split: the latest (e.g. the new pending upload) vs the
     * older "previously approved" copies preserved for reference.
     */
    private List<KycDocumentResponse> toResponsesWithLatest(List<KycDocumentEntity> docs) {
        java.util.Map<KycDocumentType, KycDocumentEntity> newestByType = new java.util.HashMap<>();
        for (KycDocumentEntity d : docs) {
            KycDocumentEntity cur = newestByType.get(d.getDocumentType());
            if (cur == null || isNewer(d, cur)) {
                newestByType.put(d.getDocumentType(), d);
            }
        }
        java.util.Set<Long> latestIds = newestByType.values().stream()
                .map(KycDocumentEntity::getId)
                .collect(Collectors.toSet());
        return docs.stream().map(d -> {
            KycDocumentResponse r = toDocumentResponse(d);
            r.setLatest(latestIds.contains(d.getId()));
            return r;
        }).collect(Collectors.toList());
    }

    /** Newer = later createdAt; falls back to higher id when timestamps tie or are missing. */
    private boolean isNewer(KycDocumentEntity a, KycDocumentEntity b) {
        if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
            int c = a.getCreatedAt().compareTo(b.getCreatedAt());
            if (c != 0) return c > 0;
        }
        return a.getId() != null && b.getId() != null && a.getId() > b.getId();
    }

    private KycDocumentResponse toDocumentResponse(KycDocumentEntity entity) {
        return KycDocumentResponse.builder()
                .id(entity.getId())
                .documentType(entity.getDocumentType())
                .documentNumber(entity.getDocumentNumber())
                .filePath(entity.getFilePath())
                .status(entity.getStatus())
                .verifiedBy(entity.getVerifiedBy() != null ? entity.getVerifiedBy().toString() : null)
                .verifiedAt(entity.getVerifiedAt())
                .rejectionReason(entity.getRejectionReason())
                .expiryDate(entity.getExpiryDate())
                .issueDate(entity.getIssueDate())
                .fileName(extractFileName(entity.getFilePath()))
                .fileUrl("/api/users/" + entity.getUserId() + "/kyc/documents/" + entity.getId() + "/file")
                .createdAt(entity.getCreatedAt())
                .realUpload(entity.getFileHash() != null && !entity.getFileHash().isBlank())
                .build();
    }

    private String extractFileName(String filePath) {
        if (filePath == null) return null;
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}
