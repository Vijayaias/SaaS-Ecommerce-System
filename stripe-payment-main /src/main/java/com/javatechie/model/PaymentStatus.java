package com.javatechie.model;

public final class PaymentStatus {
    public static final String PENDING = "PENDING";
    public static final String REQUIRES_ACTION = "REQUIRES_ACTION";
    public static final String SUCCEEDED = "SUCCEEDED";
    public static final String REFUNDED = "REFUNDED";
    public static final String FAILED = "FAILED";
    public static final String CANCELLED = "CANCELLED";

    private PaymentStatus() {
    }
}
