package com.remitm.modules.payout.nsano.dto;

import lombok.Data;

/**
 * Admin-triggered payout request body for POST /api/payout/nsano/initiate.
 * Recipient fields are taken directly in the request (no deep coupling to a
 * beneficiary entity) so this controller can be driven by an admin.
 */
@Data
public class NsanoInitiateRequest {

    /** Our transaction referenceNumber (= NSANO `reference`). */
    private String referenceNumber;

    /** "WALLET" (mobile money) or "BANK" (account). */
    private String paymentType;

    /** Mobile network/operator name for wallet (e.g. "MTN"); bank SWIFT/code for bank. */
    private String destinationHouse;

    /** Recipient mobile number (wallet) or account number (bank). */
    private String recipient;

    /** Recipient display name. */
    private String recipientName;

    /** Free-text narration / payment purpose. */
    private String narration;
}
