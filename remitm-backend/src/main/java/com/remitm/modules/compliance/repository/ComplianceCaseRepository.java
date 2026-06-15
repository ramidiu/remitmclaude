package com.remitm.modules.compliance.repository;

import com.remitm.common.enums.CaseStatus;
import com.remitm.modules.compliance.entity.ComplianceCaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComplianceCaseRepository extends JpaRepository<ComplianceCaseEntity, Long> {

    List<ComplianceCaseEntity> findByStatus(CaseStatus status);

    List<ComplianceCaseEntity> findByUserId(Long userId);

    Optional<ComplianceCaseEntity> findByCaseReference(String caseReference);

    Optional<ComplianceCaseEntity> findTopByUserIdOrderByIdDesc(Long userId);
}
