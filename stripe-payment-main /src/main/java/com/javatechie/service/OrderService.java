package com.javatechie.service;

import com.javatechie.dto.CreateOrderRequest;
import com.javatechie.dto.OrderResponse;
import com.javatechie.model.OrderEntity;
import com.javatechie.model.OrderStatus;
import com.javatechie.model.PaymentStatus;
import com.javatechie.model.UserEntity;
import com.javatechie.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public OrderService(OrderRepository orderRepository, PaymentEventPublisher paymentEventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, UserEntity user) {
        String currency = request.getCurrency().trim().toUpperCase();
        if (!currency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(BAD_REQUEST, "Currency must be a 3-letter ISO code");
        }

        LocalDateTime now = LocalDateTime.now();
        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setProductName(request.getProductName().trim());
        order.setQuantity(request.getQuantity());
        order.setUnitAmount(request.getUnitAmount());
        order.setTotalAmount(request.getUnitAmount() * request.getQuantity());
        order.setCurrency(currency);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        OrderEntity saved = orderRepository.save(order);
        paymentEventPublisher.publishOrderEvent("ORDER_CREATED", saved);
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderEntityForUser(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
    }

    @Transactional(readOnly = true)
    public OrderEntity getOrderEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order not found"));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId, Long userId) {
        return map(getOrderEntityForUser(orderId, userId));
    }

    @Transactional
    public void markPaymentPending(OrderEntity order) {
        order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
        order.setPaymentStatus(PaymentStatus.REQUIRES_ACTION);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        paymentEventPublisher.publishOrderEvent("ORDER_PAYMENT_PENDING", saved);
    }

    @Transactional
    public void markPaymentSucceeded(OrderEntity order) {
        order.setOrderStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        paymentEventPublisher.publishOrderEvent("ORDER_CONFIRMED", saved);
    }

    @Transactional
    public void markPaymentFailed(OrderEntity order) {
        order.setOrderStatus(OrderStatus.PAYMENT_FAILED);
        order.setPaymentStatus(PaymentStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        paymentEventPublisher.publishOrderEvent("ORDER_PAYMENT_FAILED", saved);
    }

    @Transactional
    public void markCancelled(OrderEntity order) {
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setUpdatedAt(LocalDateTime.now());
        OrderEntity saved = orderRepository.save(order);
        paymentEventPublisher.publishOrderEvent("ORDER_CANCELLED", saved);
    }

    private OrderResponse map(OrderEntity order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .unitAmount(order.getUnitAmount())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
