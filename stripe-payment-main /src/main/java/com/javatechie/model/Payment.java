package com.javatechie.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    private Long id;
    private Long orderId;
    private String provider; // STRIPE or RAZORPAY
    private String providerPaymentId;
    private String status;
    private String currency;
    private Long amount;
    private LocalDateTime createdAt;
    private String idempotencyKey;
}
