package com.remitm.modules.payout.zeepay.repository;

import com.remitm.modules.payout.zeepay.entity.ZeePayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZeePayRepository extends JpaRepository<ZeePayEntity, Long> {

    Optional<ZeePayEntity> findByExtraId(String extraId);

    Optional<ZeePayEntity> findByZeePayId(String zeePayId);

    Optional<ZeePayEntity> findFirstByTransactionIdOrderByIdDesc(String transactionId);

    /** Rows still in flight (have a Zeepay id and an open status) — polled by the status scheduler. */
    List<ZeePayEntity> findByZeePayIdIsNotNullAndStatusIn(List<String> statuses);
}
