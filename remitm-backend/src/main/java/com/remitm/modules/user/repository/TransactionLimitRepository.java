package com.remitm.modules.user.repository;

import com.remitm.modules.user.entity.TransactionLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionLimitRepository extends JpaRepository<TransactionLimit, Long> {

    List<TransactionLimit> findByRiskLevel(String riskLevel);

    Optional<TransactionLimit> findByRiskLevelAndLimitType(String riskLevel, String limitType);
}
