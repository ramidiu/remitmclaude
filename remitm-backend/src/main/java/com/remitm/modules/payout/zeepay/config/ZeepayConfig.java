package com.remitm.modules.payout.zeepay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bag for the Zeepay payout (mobile-money / bank / cash-pickup) integration.
 * See application.yml for the {@code zeepay.*} block.
 *
 * <p>Authentication is a static, long-lived Bearer JWT supplied out-of-band (no token
 * endpoint) — sent as {@code Authorization: Bearer <token>} on every call.
 */
@Configuration
@ConfigurationProperties(prefix = "zeepay")
@Getter
@Setter
public class ZeepayConfig {

    /** Base URL — note it ends with a trailing slash so concatenation yields {@code .../api/payouts}. */
    private String url = "https://shop.digitaltermination.com/";

    /** Static long-lived Bearer JWT used on every Zeepay call. */
    private String token = "";

    /** When false, the status-poll scheduler is a no-op. */
    private boolean schedulerEnabled = false;

    /** Status-poll scheduler fixed delay in milliseconds. */
    private long statusPollIntervalMs = 30000L;
}
