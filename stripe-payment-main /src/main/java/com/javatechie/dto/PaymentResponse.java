package com.javatechie.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String provider;
    private String providerPaymentId;
    private String refundId;
    private String providerEventId;
    private String sessionId;
    private String status;
    private String currency;
    private Long amount;
    private String idempotencyKey;
    private Boolean signatureVerified;
    private String failureReason;
    private String checkoutUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
