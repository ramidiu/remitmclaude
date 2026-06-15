package com.remitm.modules.payout.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;

/** What a gateway can do — drives the UI (whether to show live verify) without per-gateway ifs. */
@Data
@AllArgsConstructor
public class GatewayCapabilities {
    private boolean supportsNameCheck; // can resolve a recipient name before saving
    private boolean async;             // payout completes asynchronously (status poll / callback)
}
