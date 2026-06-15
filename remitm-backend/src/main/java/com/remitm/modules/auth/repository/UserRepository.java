package com.remitm.modules.auth.repository;

import com.remitm.modules.auth.entity.UserEntity;
import com.remitm.common.enums.KycTier;
import com.remitm.common.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByUuid(String uuid);

    boolean existsByEmail(String email);

    List<UserEntity> findByEmailVerifiedFalseAndCreatedAtBefore(LocalDateTime cutoff);

    Page<UserEntity> findByStatusAndKycTier(UserStatus status, KycTier kycTier, Pageable pageable);

    long countByStatus(UserStatus status);

    long countByKycTier(KycTier kycTier);

    long countByKycTierIn(List<KycTier> kycTiers);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = 'CUSTOMER' AND " +
            "(u.country = 'GB' OR u.countryCode = 'GB' OR u.countryOfResidence = 'GB' OR " +
            "u.country = 'GBR' OR u.countryCode = 'GBR')")
    List<UserEntity> findUkFrontendCustomers();

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE r.name = 'CUSTOMER' AND " +
            "(u.country IS NULL OR (u.country != 'GB' AND u.country != 'GBR')) AND " +
            "(u.countryCode IS NULL OR (u.countryCode != 'GB' AND u.countryCode != 'GBR')) AND " +
            "(u.countryOfResidence IS NULL OR (u.countryOfResidence != 'GB' AND u.countryOfResidence != 'GBR'))")
    List<UserEntity> findImportedBackendCustomers();

    @Query("SELECT u FROM UserEntity u WHERE " +
            "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:kycTier IS NULL OR u.kycTier = :kycTier) " +
            "AND (:kycStatus IS NULL " +
            "     OR (:kycStatus = 'VERIFIED' AND (u.kycTier IN (com.remitm.common.enums.KycTier.TIER_1, com.remitm.common.enums.KycTier.TIER_2, com.remitm.common.enums.KycTier.TIER_3) " +
            "          OR (u.kycTier = com.remitm.common.enums.KycTier.TIER_0 " +
            "              AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity vi WHERE vi.userId = u.id AND vi.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND vi.documentType IN (com.remitm.common.enums.KycDocumentType.NATIONAL_ID, com.remitm.common.enums.KycDocumentType.PASSPORT, com.remitm.common.enums.KycDocumentType.DRIVING_LICENCE)) " +
            "              AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity va WHERE va.userId = u.id AND va.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND va.documentType = com.remitm.common.enums.KycDocumentType.PROOF_OF_ADDRESS)))) " +
            "     OR (:kycStatus = 'REJECTED' AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity d WHERE d.userId = u.id AND d.status = com.remitm.common.enums.KycDocumentStatus.REJECTED)) " +
            // PENDING = a real (app-uploaded, file_hash set) pending document awaiting review.
            "     OR (:kycStatus = 'PENDING' AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity d WHERE d.userId = u.id AND d.fileHash IS NOT NULL AND d.status = com.remitm.common.enums.KycDocumentStatus.PENDING)) " +
            // PARTIAL = "incomplete, must still complete KYC" = TIER_0 with no real pending submission,
            // no rejected doc, and NOT a complete approved set (ID + address). Covers both no-document
            // customers and those missing one document — they all get forced to upload at login.
            "     OR (:kycStatus = 'PARTIAL' AND u.kycTier = com.remitm.common.enums.KycTier.TIER_0 " +
            "         AND NOT EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity rp WHERE rp.userId = u.id AND rp.fileHash IS NOT NULL AND rp.status = com.remitm.common.enums.KycDocumentStatus.PENDING) " +
            "         AND NOT EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity rj WHERE rj.userId = u.id AND rj.status = com.remitm.common.enums.KycDocumentStatus.REJECTED) " +
            "         AND NOT (EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity ci WHERE ci.userId = u.id AND ci.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND ci.documentType IN (com.remitm.common.enums.KycDocumentType.NATIONAL_ID, com.remitm.common.enums.KycDocumentType.PASSPORT, com.remitm.common.enums.KycDocumentType.DRIVING_LICENCE)) " +
            "                  AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity ca WHERE ca.userId = u.id AND ca.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND ca.documentType = com.remitm.common.enums.KycDocumentType.PROOF_OF_ADDRESS)))" +
            ")")
    Page<UserEntity> searchUsers(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("kycTier") KycTier kycTier,
            @Param("kycStatus") String kycStatus,
            Pageable pageable);

    // Same filters as searchUsers but with an explicit alphabetical ORDER BY that
    // pushes NULL / empty firstName rows to the bottom (MySQL has no NULLS LAST syntax).
    @Query("SELECT u FROM UserEntity u WHERE " +
            "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:kycTier IS NULL OR u.kycTier = :kycTier) " +
            "AND (:kycStatus IS NULL " +
            "     OR (:kycStatus = 'VERIFIED' AND (u.kycTier IN (com.remitm.common.enums.KycTier.TIER_1, com.remitm.common.enums.KycTier.TIER_2, com.remitm.common.enums.KycTier.TIER_3) " +
            "          OR (u.kycTier = com.remitm.common.enums.KycTier.TIER_0 " +
            "              AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity vi WHERE vi.userId = u.id AND vi.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND vi.documentType IN (com.remitm.common.enums.KycDocumentType.NATIONAL_ID, com.remitm.common.enums.KycDocumentType.PASSPORT, com.remitm.common.enums.KycDocumentType.DRIVING_LICENCE)) " +
            "              AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity va WHERE va.userId = u.id AND va.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND va.documentType = com.remitm.common.enums.KycDocumentType.PROOF_OF_ADDRESS)))) " +
            "     OR (:kycStatus = 'REJECTED' AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity d WHERE d.userId = u.id AND d.status = com.remitm.common.enums.KycDocumentStatus.REJECTED)) " +
            // PENDING = a real (app-uploaded, file_hash set) pending document awaiting review.
            "     OR (:kycStatus = 'PENDING' AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity d WHERE d.userId = u.id AND d.fileHash IS NOT NULL AND d.status = com.remitm.common.enums.KycDocumentStatus.PENDING)) " +
            // PARTIAL = "incomplete, must still complete KYC" = TIER_0 with no real pending submission,
            // no rejected doc, and NOT a complete approved set (ID + address). Covers both no-document
            // customers and those missing one document — they all get forced to upload at login.
            "     OR (:kycStatus = 'PARTIAL' AND u.kycTier = com.remitm.common.enums.KycTier.TIER_0 " +
            "         AND NOT EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity rp WHERE rp.userId = u.id AND rp.fileHash IS NOT NULL AND rp.status = com.remitm.common.enums.KycDocumentStatus.PENDING) " +
            "         AND NOT EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity rj WHERE rj.userId = u.id AND rj.status = com.remitm.common.enums.KycDocumentStatus.REJECTED) " +
            "         AND NOT (EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity ci WHERE ci.userId = u.id AND ci.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND ci.documentType IN (com.remitm.common.enums.KycDocumentType.NATIONAL_ID, com.remitm.common.enums.KycDocumentType.PASSPORT, com.remitm.common.enums.KycDocumentType.DRIVING_LICENCE)) " +
            "                  AND EXISTS (SELECT 1 FROM com.remitm.modules.user.entity.KycDocumentEntity ca WHERE ca.userId = u.id AND ca.status = com.remitm.common.enums.KycDocumentStatus.APPROVED AND ca.documentType = com.remitm.common.enums.KycDocumentType.PROOF_OF_ADDRESS)))" +
            ") " +
            "ORDER BY CASE WHEN u.firstName IS NULL OR u.firstName = '' THEN 1 ELSE 0 END ASC, " +
            "LOWER(u.firstName) ASC, LOWER(u.lastName) ASC")
    Page<UserEntity> searchUsersAlpha(
            @Param("search") String search,
            @Param("status") UserStatus status,
            @Param("kycTier") KycTier kycTier,
            @Param("kycStatus") String kycStatus,
            Pageable pageable);
}
