package com.javatechie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payments_idempotency_key", columnNames = "idempotencyKey")
        })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(length = 120)
    private String providerPaymentId;

    @Column(length = 120)
    private String refundId;

    @Column(length = 120)
    private String providerEventId;

    @Column(length = 120)
    private String sessionId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false, length = 120)
    private String idempotencyKey;

    @Column(nullable = false)
    private Boolean signatureVerified;

    @Column(length = 255)
    private String failureReason;
}
