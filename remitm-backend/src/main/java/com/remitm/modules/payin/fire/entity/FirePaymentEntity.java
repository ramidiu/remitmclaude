package com.remitm.modules.payin.fire.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fire_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirePaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Our internal transaction reference number (TransactionEntity.referenceNumber). */
    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    /** The payment-request code returned by Fire (used to build the hosted redirect URL). */
    @Column(name = "fire_code", length = 100)
    private String fireCode;

    /** The payment UUID returned by Fire once the payer has paid. */
    @Column(name = "payment_uuid", length = 100)
    private String paymentUuid;

    @Column(name = "ican_to", length = 50)
    private String icanTo;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "my_reference", length = 100)
    private String myReference;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "return_url", length = 255)
    private String returnUrl;

    @Builder.Default
    @Column(name = "status", length = 50)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
