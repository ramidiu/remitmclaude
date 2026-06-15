package com.remitm.modules.payout.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Result of resolving a corridor + delivery method to its active payout partner + gateway. */
@Data
@AllArgsConstructor
public class PayoutRoute {
    private Long corridorId;
    private Long payoutPartnerId;
    private String partnerName;
    private String gateway;                 // NSANO | ZEEPAY | MANUAL | ...
    private GatewayCapabilities capabilities;
}
