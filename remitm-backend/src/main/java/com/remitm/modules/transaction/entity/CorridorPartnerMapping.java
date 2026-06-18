package com.remitm.modules.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "corridor_partner_mappings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorridorPartnerMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    /** Receive country (ISO-3) — disambiguates corridors that share a currency (XOF/XAF). */
    @Column(name = "receive_country", length = 3)
    private String receiveCountry;

    @Column(name = "partner_id", nullable = false)
    private Long partnerId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isActive == null) isActive = true;
    }
}
