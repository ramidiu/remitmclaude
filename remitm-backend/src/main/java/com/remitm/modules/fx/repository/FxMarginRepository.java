package com.remitm.modules.fx.repository;

import com.remitm.common.enums.DeliveryMethod;
import com.remitm.common.enums.KycTier;
import com.remitm.modules.fx.entity.FxMarginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FxMarginRepository extends JpaRepository<FxMarginEntity, Long> {

    Optional<FxMarginEntity> findBySendCurrencyAndReceiveCurrencyAndDeliveryMethodAndCustomerTierAndIsActiveTrue(
            String sendCurrency, String receiveCurrency, DeliveryMethod deliveryMethod, KycTier customerTier);

    Optional<FxMarginEntity> findBySendCurrencyAndReceiveCurrencyAndDeliveryMethodAndCustomerTierIsNullAndIsActiveTrue(
            String sendCurrency, String receiveCurrency, DeliveryMethod deliveryMethod);

    Optional<FxMarginEntity> findFirstBySendCurrencyAndReceiveCurrencyAndDeliveryMethodIsNullAndCustomerTierIsNullAndIsActiveTrue(
            String sendCurrency, String receiveCurrency);

    List<FxMarginEntity> findBySendCurrencyAndReceiveCurrencyAndIsActiveTrue(
            String sendCurrency, String receiveCurrency);

    List<FxMarginEntity> findByIsActiveTrue();
}
