package com.remitm.modules.payin.fire.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Fire (fire.com) Open Banking pay-in integration.
 *
 * <p>Defaults point at the Fire PREPROD/test environment for both the API
 * and the hosted payments host.
 */
@Configuration
@ConfigurationProperties(prefix = "fire")
@Data
public class FireProperties {

    /** Base URL of the Fire REST API. */
    private String apiBase = "https://api-preprod.fire.com";

    /** Base URL of the hosted payments host the payer is redirected to. */
    private String paymentsBase = "https://payments-preprod.fire.com";

    /** Fire application client id. */
    private String clientId = "";

    /** Fire application refresh token. */
    private String refreshToken = "";

    /** Fire application client key, used to derive the per-request client secret. */
    private String clientKey = "";

    /** Destination ICAN (Fire account) the payment request credits. */
    private String icanTo = "52363";

    /** URL the payer is redirected back to after completing the payment. */
    private String returnUrl = "http://localhost:8096/home/trust-callback";

    /** Token endpoint. Derived from {@link #apiBase} unless explicitly overridden. */
    private String tokenUrl;

    public String getTokenUrl() {
        return (tokenUrl != null && !tokenUrl.isBlank())
                ? tokenUrl
                : apiBase + "/business/v1/apps/accesstokens";
    }
}
