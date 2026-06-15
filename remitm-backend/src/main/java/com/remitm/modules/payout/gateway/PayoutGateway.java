package com.remitm.modules.payout.gateway;

import java.util.Map;

/**
 * Strategy interface every payout gateway implements (Nsano, Zeepay, Manual, future Thunes/…).
 * The customer flow and payout screen talk only to this via the facade — they never name a gateway.
 * Adding a new gateway = one new implementation; nothing else changes.
 */
public interface PayoutGateway {

    /** Stable key matching payout_partners.gateway, e.g. "NSANO", "ZEEPAY", "MANUAL". */
    String getType();

    GatewayCapabilities getCapabilities();

    /** Resolve the recipient's account-holder name (or notFound). */
    ValidationResult validateRecipient(ValidationRequest req);

    /** Initiate the payout for an existing transaction; returns a normalized result map. */
    Map<String, Object> disburse(String referenceNumber);
}
