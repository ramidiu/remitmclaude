package com.remitm.modules.payout.zeepay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * Zeepay initiate-payout response: {@code { code, message, zeepay_id, data:{...} }}.
 * Success is signalled by the STRING code {@code "411"}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZeepayInitiateResponse {

    private String code;
    private String message;

    @JsonProperty("zeepay_id")
    private String zeepayId;

    private Map<String, Object> data;
}
