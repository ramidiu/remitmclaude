package com.remitm.common.dto;

import com.remitm.common.enums.KycTier;
import com.remitm.common.enums.UserStatus;
import com.remitm.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListRequest {

    private Integer page;
    private Integer size;
    private String search;
    private UserStatus status;
    private KycTier kycTier;
    private UserType userType;
    private String sortBy;
    private String sortDir;
}
