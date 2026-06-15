package com.remitm.common.dto;

import com.remitm.common.enums.KycDocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDocumentReviewRequest {

    @NotNull(message = "Status is required")
    private KycDocumentStatus status;

    private String rejectionReason;
}
