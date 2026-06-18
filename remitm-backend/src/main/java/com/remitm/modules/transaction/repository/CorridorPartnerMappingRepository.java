package com.remitm.modules.transaction.repository;

import com.remitm.modules.transaction.entity.CorridorPartnerMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorridorPartnerMappingRepository extends JpaRepository<CorridorPartnerMapping, Long> {

    List<CorridorPartnerMapping> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);

    /** Country-scoped mapping (disambiguates currencies shared by multiple countries). */
    List<CorridorPartnerMapping> findByFromCurrencyAndToCurrencyAndReceiveCountry(
            String fromCurrency, String toCurrency, String receiveCountry);

    List<CorridorPartnerMapping> findByPartnerId(Long partnerId);
}
