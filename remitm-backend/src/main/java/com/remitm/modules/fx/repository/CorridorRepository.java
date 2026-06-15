package com.remitm.modules.fx.repository;

import com.remitm.modules.fx.entity.CorridorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorridorRepository extends JpaRepository<CorridorEntity, Long> {

    List<CorridorEntity> findByIsActiveTrue();

    Optional<CorridorEntity> findBySendCurrencyAndReceiveCurrencyAndIsActiveTrue(
            String sendCurrency, String receiveCurrency);

    Optional<CorridorEntity> findBySendCurrencyAndReceiveCurrency(
            String sendCurrency, String receiveCurrency);

    List<CorridorEntity> findByReceiveCurrencyAndIsActiveTrue(String receiveCurrency);
}
