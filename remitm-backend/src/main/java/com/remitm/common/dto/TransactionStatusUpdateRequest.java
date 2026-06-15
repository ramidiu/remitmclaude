package com.remitm.common.dto;

import com.remitm.common.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusUpdateRequest {

    @NotNull(message = "Status is required")
    private TransactionStatus status;

    private String reason;
}
