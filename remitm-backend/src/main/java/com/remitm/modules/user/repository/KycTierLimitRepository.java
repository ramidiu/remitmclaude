package com.remitm.modules.user.repository;

import com.remitm.common.enums.KycTier;
import com.remitm.modules.user.entity.KycTierLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycTierLimitRepository extends JpaRepository<KycTierLimitEntity, Long> {

    Optional<KycTierLimitEntity> findByTierAndCurrency(KycTier tier, String currency);

    List<KycTierLimitEntity> findByTier(KycTier tier);
}
