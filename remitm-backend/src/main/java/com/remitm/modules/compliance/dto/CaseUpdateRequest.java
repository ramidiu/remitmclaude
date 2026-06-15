package com.remitm.modules.compliance.dto;

import com.remitm.common.enums.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseUpdateRequest {

    private CaseStatus status;
    private Long assignedTo;
    private String summary;
    private String findings;
    private String outcome;
}
