package com.remitm.modules.fx.dto;

import com.remitm.common.enums.DeliveryMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryMethodResponse {

    private Long id;
    private DeliveryMethod deliveryMethod;
    private Long payoutPartnerId;
    private Boolean isActive;
    private Integer processingTimeMinutes;
}
