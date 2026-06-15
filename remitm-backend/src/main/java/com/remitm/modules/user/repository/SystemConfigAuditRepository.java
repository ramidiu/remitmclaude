package com.remitm.modules.user.repository;

import com.remitm.modules.user.entity.SystemConfigAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Code added by Naresh: System Controls Phase 3 — read-mostly audit lookup.
 */
@Repository
public interface SystemConfigAuditRepository extends JpaRepository<SystemConfigAudit, Long> {

    List<SystemConfigAudit> findByConfigKeyOrderByChangedAtDescIdDesc(String configKey);
}
