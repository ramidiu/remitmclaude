package com.remitm.modules.payout.zeepay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistent record of a Zeepay payout disbursement (wallet / bank / cash pickup).
 * One row per initiate attempt; updated in place as the payout is polled to completion.
 */
@Entity
@Table(name = "zee_pay")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZeePayEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Zeepay's own transaction id, returned on initiate. */
    @Column(name = "zee_pay_id", length = 100)
    private String zeePayId;

    /** Our unique idempotency reference sent as {@code extr_id}. */
    @Column(name = "extra_id", length = 100)
    private String extraId;

    /** Internal transaction reference number. */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /** Wallet | Bank | Pickup. */
    @Column(name = "service_type", length = 30)
    private String serviceType;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "amount_charged", precision = 18, scale = 4)
    private BigDecimal amountCharged;

    @Column(name = "amount_sent", precision = 18, scale = 4)
    private BigDecimal amountSent;

    @Column(name = "amount_pay_out", precision = 18, scale = 4)
    private BigDecimal amountPayOut;

    @Column(name = "status_code", length = 20)
    private String statusCode;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "sender_country", length = 10)
    private String senderCountry;

    @Column(name = "sender_first_name", length = 100)
    private String senderFirstName;

    @Column(name = "sender_last_name", length = 100)
    private String senderLastName;

    @Column(name = "recipient_first_name", length = 100)
    private String recipientFirstName;

    @Column(name = "recipient_last_name", length = 100)
    private String recipientLastName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (created == null) created = now;
        if (lastUpdated == null) lastUpdated = now;
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        LocalDateTime now = LocalDateTime.now();
        lastUpdated = now;
        updatedAt = now;
    }
}
