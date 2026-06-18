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

    // ── Country-aware / deterministic finders ──────────────────────────────
    // Multiple corridors can share a (sendCurrency, receiveCurrency) pair when countries
    // share a currency (XOF, XAF). These avoid NonUniqueResult and let callers pick the
    // exact corridor by receive country.

    /** Deterministic single corridor for a currency pair (lowest id) — safe fallback, never throws. */
    Optional<CorridorEntity> findFirstBySendCurrencyAndReceiveCurrencyAndIsActiveTrueOrderByIdAsc(
            String sendCurrency, String receiveCurrency);

    /** Deterministic single corridor for a currency pair regardless of active state. */
    Optional<CorridorEntity> findFirstBySendCurrencyAndReceiveCurrencyOrderByIdAsc(
            String sendCurrency, String receiveCurrency);

    /** Exact corridor by currency pair + receive country (ISO-3). */
    Optional<CorridorEntity> findFirstBySendCurrencyAndReceiveCurrencyAndReceiveCountryAndIsActiveTrueOrderByIdAsc(
            String sendCurrency, String receiveCurrency, String receiveCountry);

    /** Resolve currency / corridor from a receive country alone (ISO-3). */
    Optional<CorridorEntity> findFirstByReceiveCountryAndIsActiveTrueOrderByIdAsc(String receiveCountry);
}
