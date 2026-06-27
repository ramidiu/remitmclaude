package com.remitm.modules.payout.nsano.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Source IPs allowed to call the public inbound callback ({@code POST /nsano/callback}).
     * NSANO sends callback responses only from these hosts. If the list is empty the IP check
     * is disabled (fail-open) — used for local/staging where the source IP is unknown.
     */
    private List<String> callbackAllowedIps = new ArrayList<>();
}
