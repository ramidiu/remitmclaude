package com.remitm.common.dto;

import com.remitm.common.enums.EntityType;
import com.remitm.common.enums.ScreeningListType;
import com.remitm.common.enums.ScreeningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResponse {

    private Long id;
    private EntityType entityType;
    private Long entityId;
    private ScreeningListType listChecked;
    private BigDecimal matchScore;
    private ScreeningStatus status;
    private String matchDetails;
}
