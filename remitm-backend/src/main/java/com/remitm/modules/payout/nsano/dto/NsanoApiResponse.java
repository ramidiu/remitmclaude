package com.remitm.modules.payout.nsano.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Generic NSANO envelope: {@code { code, msg, data:{ transactionId, status, code, msg, accountName } }}.
 * code "00" indicates success.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NsanoApiResponse {

    private String code;
    private String msg;
    private NsanoData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NsanoData {
        private String transactionId;
        private String status;
        private String code;
        private String msg;
        private String accountName;
    }
}
