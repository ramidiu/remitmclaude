package com.remitm.common.util;

import java.util.Map;

/**
 * ISO country-code conversion (alpha-3 ⇄ alpha-2). Corridors store receive_country as alpha-3
 * (e.g. CMR) while payout_types / payment_methods use alpha-2 (e.g. CM). This is the single place
 * that reconciles the two so per-COUNTRY matching works even when several countries share a
 * currency (XOF, XAF). Reference data only — no business logic.
 */
public final class CountryCodes {

    private CountryCodes() {}

    private static final Map<String, String> A3_TO_A2 = Map.ofEntries(
            Map.entry("GBR", "GB"), Map.entry("IND", "IN"), Map.entry("PAK", "PK"),
            Map.entry("NGA", "NG"), Map.entry("GHA", "GH"), Map.entry("PHL", "PH"),
            Map.entry("AUS", "AU"), Map.entry("NPL", "NP"), Map.entry("BGD", "BD"),
            Map.entry("KEN", "KE"), Map.entry("ZAF", "ZA"), Map.entry("LKA", "LK"),
            Map.entry("USA", "US"), Map.entry("ARE", "AE"), Map.entry("DEU", "DE"),
            Map.entry("SDN", "SD"), Map.entry("TUR", "TR"), Map.entry("EGY", "EG"),
            Map.entry("SAU", "SA"), Map.entry("QAT", "QA"), Map.entry("UGA", "UG"),
            // African corridor additions
            Map.entry("ZMB", "ZM"), Map.entry("ZWE", "ZW"), Map.entry("RWA", "RW"),
            Map.entry("CIV", "CI"), Map.entry("CMR", "CM"), Map.entry("TZA", "TZ"),
            Map.entry("SEN", "SN"), Map.entry("MOZ", "MZ"), Map.entry("SLE", "SL"),
            Map.entry("BFA", "BF"), Map.entry("NER", "NE"), Map.entry("GIN", "GN"),
            Map.entry("GAB", "GA"), Map.entry("MWI", "MW"), Map.entry("COD", "CD"),
            Map.entry("BEN", "BJ"), Map.entry("BDI", "BI"), Map.entry("TGO", "TG"),
            Map.entry("TCD", "TD"), Map.entry("ETH", "ET")
    );

    /** Normalize any alpha-2/alpha-3 country code to alpha-2 (upper-case), or null if unknown/blank. */
    public static String toAlpha2(String code) {
        if (code == null || code.isBlank()) return null;
        String c = code.trim().toUpperCase();
        if (c.length() == 2) return c;
        String a2 = A3_TO_A2.get(c);
        return a2 != null ? a2 : c;
    }
}
