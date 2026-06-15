package com.remitm.modules.compliance.repository;

import com.remitm.modules.compliance.entity.SarReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SarReportRepository extends JpaRepository<SarReportEntity, Long> {

    List<SarReportEntity> findByComplianceCaseId(Long caseId);
}
