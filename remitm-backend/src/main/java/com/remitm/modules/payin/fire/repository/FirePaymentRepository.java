package com.remitm.modules.payin.fire.repository;

import com.remitm.modules.payin.fire.entity.FirePaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FirePaymentRepository extends JpaRepository<FirePaymentEntity, Long> {
    Optional<FirePaymentEntity> findByTransactionId(String transactionId);

    Optional<FirePaymentEntity> findByFireCode(String fireCode);
}
