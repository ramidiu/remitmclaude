package com.remitm.modules.payout.zeepay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Zeepay status-check response. The transaction status lives at {@code data.status};
 * a value of "Success" (case-insensitive) means the payout has been paid out.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeepayStatusResponse {

    private String code;
    private String message;
    private Map<String, Object> data;

    /** Convenience accessor for {@code data.status}, or {@code null} if absent. */
    public String getStatus() {
        if (data == null) return null;
        Object status = data.get("status");
        return status != null ? status.toString() : null;
    }
}
