package com.remitm.modules.payout.gateway;

import lombok.Builder;
import lombok.Data;

/** Gateway-agnostic recipient-validation request. Each gateway picks the fields it needs. */
@Data
@Builder
public class ValidationRequest {
    private String deliveryMethod;    // BANK_DEPOSIT | MOBILE_WALLET | CASH_PICKUP
    private String accountNumber;     // bank account number OR mobile number
    private String bankOrProvider;    // bank code (e.g. Nsano destinationHouse "NIB") OR mobile network ("MTN")
    private String routingNumber;     // bank routing number (Zeepay bank validation uses this)
    private String receivingCountry;  // ISO-2, e.g. GH
}
