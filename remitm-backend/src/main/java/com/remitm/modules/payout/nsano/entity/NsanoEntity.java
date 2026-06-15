package com.remitm.modules.payout.nsano.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persisted record of an NSANO payout (one row per disbursement attempt).
 * transaction_id holds our own transaction referenceNumber; nsano_transaction_id
 * holds the id NSANO returns and which we use for status polling / callbacks.
 */
@Entity
@Table(name = "nsano")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NsanoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our own transaction referenceNumber. */
    @Column(name = "transaction_id", nullable = false, length = 100)
    private String transactionId;

    /** Id returned by NSANO (used for status polling and callback correlation). */
    @Column(name = "nsano_transaction_id", length = 100)
    private String nsanoTransactionId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "code", length = 20)
    private String code;

    @Builder.Default
    @Column(name = "status", length = 30)
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "api_status", length = 30)
    private String apiStatus = "notdone";

    @Column(name = "sender_name", length = 255)
    private String senderName;

    @Column(name = "recipient_name", length = 255)
    private String recipientName;

    @Column(name = "sender_account", length = 100)
    private String senderAccount;

    @Column(name = "recipient_account", length = 100)
    private String recipientAccount;

    @Column(name = "source_currency", length = 10)
    private String sourceCurrency;

    @Column(name = "dest_currency", length = 10)
    private String destCurrency;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "rate", precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
