package com.remitm.common.dto;

import com.remitm.common.enums.FxRateSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRateResponse {

    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private FxRateSource source;
    private LocalDateTime fetchedAt;
}
