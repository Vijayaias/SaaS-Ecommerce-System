package com.javatechie.controller;

import com.javatechie.dto.CreateOrderRequest;
import com.javatechie.dto.OrderResponse;
import com.javatechie.dto.ApiMessageResponse;
import com.javatechie.model.UserEntity;
import com.javatechie.service.OrderService;
import com.javatechie.service.PaymentService;
import com.javatechie.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UserService userService;

    public OrderController(OrderService orderService, PaymentService paymentService, UserService userService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return orderService.createOrder(request, user);
    }

    @GetMapping
    public List<OrderResponse> getOrders(Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return orderService.getOrdersForUser(user.getId());
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return orderService.getOrder(orderId, user.getId());
    }

    @PostMapping("/{orderId}/cancel")
    public ApiMessageResponse cancelOrder(@PathVariable Long orderId, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        paymentService.cancelConfirmedOrder(orderId, user);
        return new ApiMessageResponse("Order cancelled and refund initiated successfully");
    }
}
