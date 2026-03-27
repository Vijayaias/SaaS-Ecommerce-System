package com.javatechie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 120)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long unitAmount;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false, length = 30)
    private String orderStatus;

    @Column(nullable = false, length = 30)
    private String paymentStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
