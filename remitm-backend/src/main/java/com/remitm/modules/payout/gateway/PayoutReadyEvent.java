package com.remitm.modules.payout.gateway;

/**
 * Published the moment a transaction reaches FUNDS_RECEIVED — i.e. the money is in and the
 * recipient can be paid. The {@link AutoPayoutListener} reacts (after commit) and auto-disburses
 * through the corridor's resolved gateway, replicating the old laylaremitm flow where payout fired
 * automatically once funds were confirmed (MANUAL corridors are skipped — an operator pays those).
 *
 * @param referenceNumber the transaction's reference (e.g. "TXN109470")
 */
public record PayoutReadyEvent(String referenceNumber) { }
