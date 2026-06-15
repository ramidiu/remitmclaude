package com.remitm.modules.compliance.dto;

import com.remitm.common.enums.EntityType;
import com.remitm.common.enums.ScreeningListType;
import com.remitm.common.enums.ScreeningStatus;
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
public class ScreeningResponse {

    private Long id;
    private EntityType entityType;
    private Long entityId;
    private String screenedName;
    private ScreeningListType matchedList;
    private Long matchedEntryId;
    private BigDecimal matchScore;
    private ScreeningStatus status;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String notes;
    private LocalDateTime createdAt;

    private String listType;
    private String sourceCode;
    private String matchedEntryName;
    private String matchedTopics;
    private boolean hit;
}
