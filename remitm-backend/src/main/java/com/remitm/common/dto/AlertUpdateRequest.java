package com.remitm.common.dto;

import com.remitm.common.enums.AlertStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertUpdateRequest {

    private AlertStatus status;
    private String assignedTo;
    private String notes;
}
