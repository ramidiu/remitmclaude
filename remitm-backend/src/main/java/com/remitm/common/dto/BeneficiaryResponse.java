package com.remitm.common.dto;

import com.remitm.common.enums.DeliveryMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryResponse {

    private Long id;
    private String fullName;
    private String country;
    private DeliveryMethod deliveryMethod;
    private String payoutGateway;
    private String bankName;
    private String accountNumber;
    private String iban;
    private String swiftBic;
    private String sortCode;
    private String branchState;
    private String branchCity;
    private String mobileNumber;
    private String mobileProvider;
    private String idType;
    private String idNumber;
    private String address;
    private String relationship;
    private Boolean isFavourite;
    private LocalDateTime createdAt;
}
