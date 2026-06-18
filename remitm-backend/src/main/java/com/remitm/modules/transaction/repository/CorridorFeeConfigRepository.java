package com.remitm.modules.transaction.repository;

import com.remitm.modules.transaction.entity.CorridorFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorridorFeeConfigRepository extends JpaRepository<CorridorFeeConfig, Long> {

    List<CorridorFeeConfig> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);

    /** Country-scoped config (disambiguates currencies shared by multiple countries). */
    List<CorridorFeeConfig> findByFromCurrencyAndToCurrencyAndReceiveCountry(
            String fromCurrency, String toCurrency, String receiveCountry);
}
