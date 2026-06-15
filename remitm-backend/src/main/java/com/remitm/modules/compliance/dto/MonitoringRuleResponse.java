package com.remitm.modules.compliance.dto;

import com.remitm.common.enums.AlertSeverity;
import com.remitm.common.enums.MonitoringRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringRuleResponse {

    private Long id;
    private String ruleName;
    private MonitoringRuleType ruleType;
    private String parameters;
    private AlertSeverity severity;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
