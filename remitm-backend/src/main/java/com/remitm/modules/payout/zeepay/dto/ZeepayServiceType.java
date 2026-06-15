package com.remitm.modules.payout.zeepay.dto;

/**
 * The three Zeepay disbursement flavours. All hit the same {@code api/payouts} endpoint,
 * differing only by the {@code service_type} form field carried by {@link #getApiValue()}.
 */
public enum ZeepayServiceType {
    WALLET("Wallet"),
    BANK("Bank"),
    PICKUP("Pickup");

    private final String apiValue;

    ZeepayServiceType(String apiValue) {
        this.apiValue = apiValue;
    }

    /** The exact {@code service_type} string Zeepay expects. */
    public String getApiValue() {
        return apiValue;
    }
}
