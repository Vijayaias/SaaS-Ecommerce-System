package com.javatechie.controller;

import com.javatechie.dto.CreateOrderRequest;
import com.javatechie.dto.CreatePaymentRequest;
import com.javatechie.dto.ProductRequest;
import com.javatechie.dto.PaymentResponse;
import com.javatechie.model.UserEntity;
import com.javatechie.service.OrderService;
import com.javatechie.service.PaymentService;
import com.javatechie.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/product/v1")
public class ProductCheckoutController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UserService userService;

    public ProductCheckoutController(OrderService orderService, PaymentService paymentService, UserService userService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping("/checkout")
    public PaymentResponse checkoutProducts(@RequestBody ProductRequest productRequest, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setProductName(productRequest.getName());
        orderRequest.setQuantity(productRequest.getQuantity().intValue());
        orderRequest.setUnitAmount(productRequest.getAmount());
        orderRequest.setCurrency(productRequest.getCurrency() == null ? "USD" : productRequest.getCurrency());

        Long orderId = orderService.createOrder(orderRequest, user).getId();

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest();
        paymentRequest.setOrderId(orderId);
        paymentRequest.setProvider("STRIPE");
        paymentRequest.setIdempotencyKey(UUID.randomUUID().toString());

        return paymentService.createPayment(paymentRequest, user);
    }
}
