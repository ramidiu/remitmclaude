package com.remitm.modules.payout.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Normalized validation result — every gateway maps its own response into this shape. */
@Data
@AllArgsConstructor
public class ValidationResult {
    private boolean found;
    private String accountName;

    public static ValidationResult notFound() {
        return new ValidationResult(false, null);
    }

    public static ValidationResult of(String name) {
        boolean ok = name != null && !name.isBlank();
        return new ValidationResult(ok, ok ? name : null);
    }
}
