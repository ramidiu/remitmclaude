package com.remitm.modules.fx.dto;

import com.remitm.common.enums.KycTier;
import com.remitm.common.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorridorUpdateRequest {

    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private BigDecimal dailyLimit;
    private BigDecimal monthlyLimit;
    private KycTier requiredKycTier;
    private RiskLevel riskLevel;
}
