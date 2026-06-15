package com.remitm.modules.payout.zeepay.dto;

import lombok.Data;

/**
 * Inbound request to initiate a Zeepay payout. Recipient details (mobile number, network,
 * bank account, etc.) are supplied directly on the request rather than coupled to a
 * beneficiary entity.
 */
@Data
public class ZeepayInitiateRequest {

    /** Internal transaction reference number — the entity must already exist. */
    private String referenceNumber;

    /** WALLET | BANK | PICKUP. */
    private ZeepayServiceType serviceType;

    // --- Recipient identity ---
    private String receiverFirstName;
    private String receiverLastName;
    private String receiverCountry;
    private String address;

    // --- Wallet / Pickup ---
    /** Dialing code, e.g. "233" (concatenated with the mobile/phone number). */
    private String dialingCode;
    /** Recipient mobile / MSISDN (without dialing code). */
    private String recipientMsisdn;
    /** Mobile network operator (Wallet only). */
    private String mno;

    // --- Bank ---
    private String accountNumber;
    private String routingNumber;

    // --- Sender overrides (optional; fall back to the transaction's sender) ---
    private String senderFirstName;
    private String senderLastName;
}
