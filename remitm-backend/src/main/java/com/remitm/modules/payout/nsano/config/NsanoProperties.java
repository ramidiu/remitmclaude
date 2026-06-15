package com.remitm.modules.payout.nsano.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration bag for the NSANO Ghana payout (mobile-money / bank disbursement) integration.
 * See application.yml for the nsano.* block.
 */
@Configuration
@ConfigurationProperties(prefix = "nsano")
@Getter
@Setter
public class NsanoProperties {

    /** NSANO API base URL (no trailing slash). */
    private String baseUrl = "https://staging.instantcredit.services";

    /** API key sent on every request via the custom Authorization-Key header. */
    private String apiKey = "";

    /** When false, the status-poll scheduler is a no-op. */
    private boolean schedulerEnabled = false;

    /** Status-poll scheduler fixed delay in milliseconds. */
    private long statusPollIntervalMs = 300000L;
}
