package com.remitm.modules.compliance.repository;

import com.remitm.common.enums.EntityType;
import com.remitm.modules.compliance.entity.RiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RiskScoreRepository extends JpaRepository<RiskScoreEntity, Long> {

    Optional<RiskScoreEntity> findTopByEntityTypeAndEntityIdOrderByCalculatedAtDesc(EntityType entityType, Long entityId);
}
