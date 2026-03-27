package com.javatechie.controller;

import com.javatechie.dto.ApiMessageResponse;
import com.javatechie.dto.CreatePaymentRequest;
import com.javatechie.dto.PaymentResponse;
import com.javatechie.model.UserEntity;
import com.javatechie.service.PaymentService;
import com.javatechie.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    @PostMapping
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return paymentService.createPayment(request, user);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable Long paymentId, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return paymentService.getPaymentById(paymentId, user);
    }

    @GetMapping("/order/{orderId}")
    public List<PaymentResponse> getPaymentsByOrder(@PathVariable Long orderId, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return paymentService.getPaymentsForOrder(orderId, user);
    }

    @PostMapping("/confirm/{sessionId}")
    public ApiMessageResponse confirmStripePayment(@PathVariable String sessionId, Principal principal) {
        UserEntity user = userService.getByUsername(principal.getName());
        return paymentService.confirmStripeCheckoutSession(sessionId, user);
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<ApiMessageResponse> handleStripeWebhook(@RequestHeader("Stripe-Signature") String signature,
                                                                  @RequestBody String payload) {
        paymentService.processStripeWebhook(payload, signature);
        return ResponseEntity.ok(new ApiMessageResponse("Webhook processed"));
    }
}
