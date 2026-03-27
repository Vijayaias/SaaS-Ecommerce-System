package com.javatechie.model;

public final class OrderStatus {
    public static final String CREATED = "CREATED";
    public static final String PAYMENT_PENDING = "PAYMENT_PENDING";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
    public static final String CANCELLED = "CANCELLED";

    private OrderStatus() {
    }
}
