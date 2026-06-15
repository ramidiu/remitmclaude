package com.remitm.common.dto;

import com.remitm.common.enums.AccountStatus;
import com.remitm.common.enums.KycTier;
import com.remitm.common.enums.UserStatus;
import com.remitm.common.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String uuid;
    private String email;
    private String phone;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private UserType userType;
    private KycTier kycTier;
    private UserStatus status;
    private AccountStatus accountStatus;
    private LocalDateTime deleteRequestedAt;
    private String country;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String postcode;
    private String preferredLanguage;
    private Boolean mfaEnabled;
    private java.util.List<String> roles;
    private LocalDateTime createdAt;
}
