package com.remitm.modules.payin.fire.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request/response DTOs for the Fire (fire.com) Open Banking pay-in module.
 */
public final class FireDtos {

    private FireDtos() {
    }

    /** Request body for POST /api/fire/payment-request. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequestBody {
        private String referenceNumber;
    }

    /** Request body for POST /api/fire/update-payment. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdatePaymentBody {
        private String referenceNumber;
        private String paymentUuid;
    }

    /** Response body for POST /api/fire/payment-request. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRequestResponse {
        private String url;
        private String code;
    }
}
