package com.remitm.modules.compliance.dto;

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
    private Long assignedTo;
    private String resolutionNotes;
}
