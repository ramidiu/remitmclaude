package com.remitm.modules.transaction.dto;

import com.remitm.common.enums.PaymentMethodType;
import com.remitm.common.enums.PaymentStatus;
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
public class PaymentResponse {

    private Long id;
    private Long transactionId;
    private PaymentMethodType methodType;
    private String provider;
    private String providerReference;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
