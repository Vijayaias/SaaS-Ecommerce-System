package com.javatechie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotNull
    private Long orderId;

    @NotBlank
    private String provider;

    @NotBlank
    private String idempotencyKey;
}
