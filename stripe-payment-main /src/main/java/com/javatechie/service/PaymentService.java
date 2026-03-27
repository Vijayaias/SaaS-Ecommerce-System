package com.javatechie.service;

import com.javatechie.dto.ApiMessageResponse;
import com.javatechie.dto.CreatePaymentRequest;
import com.javatechie.dto.PaymentResponse;
import com.javatechie.model.OrderEntity;
import com.javatechie.model.PaymentEntity;
import com.javatechie.model.PaymentStatus;
import com.javatechie.model.UserEntity;
import com.javatechie.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final StripeService stripeService;
    private final RedisStateService redisStateService;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderService orderService,
                          StripeService stripeService,
                          RedisStateService redisStateService,
                          PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
        this.stripeService = stripeService;
        this.redisStateService = redisStateService;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request, UserEntity user) {
        String provider = request.getProvider().trim().toUpperCase(Locale.ROOT);
        if (!"STRIPE".equals(provider) && !"RAZORPAY".equals(provider)) {
            throw new ResponseStatusException(BAD_REQUEST, "Unsupported provider");
        }

        PaymentEntity existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return map(existing, null);
        }

        boolean reserved = redisStateService.tryReservePaymentIdempotency(request.getIdempotencyKey());
        if (!reserved) {
            existing = paymentRepository.findByIdempotencyKey(request.getIdempotencyKey()).orElse(null);
            if (existing != null) {
                return map(existing, null);
            }
            throw new ResponseStatusException(BAD_REQUEST, "Payment request is already being processed");
        }

        try {
            PaymentResponse response = createNewPayment(request, user, provider);
            redisStateService.markPaymentIdempotencyCompleted(request.getIdempotencyKey(), String.valueOf(response.getId()));
            return response;
        } catch (RuntimeException ex) {
            redisStateService.clearPaymentIdempotency(request.getIdempotencyKey());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForOrder(Long orderId, UserEntity user) {
        orderService.getOrderEntityForUser(orderId, user.getId());
        return paymentRepository.findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(payment -> map(payment, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId, UserEntity user) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment not found"));
        orderService.getOrderEntityForUser(payment.getOrderId(), user.getId());
        return map(payment, null);
    }

    @Transactional
    public ApiMessageResponse confirmStripeCheckoutSession(String sessionId, UserEntity user) {
        PaymentEntity payment = paymentRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment session not found"));
        OrderEntity order = orderService.getOrderEntityForUser(payment.getOrderId(), user.getId());

        if (PaymentStatus.SUCCEEDED.equals(payment.getStatus()) || PaymentStatus.REFUNDED.equals(payment.getStatus())) {
            return new ApiMessageResponse("Payment already confirmed");
        }

        Session session = stripeService.retrieveCheckoutSession(sessionId);
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Stripe session is not paid yet");
        }

        payment.setProviderPaymentId(session.getPaymentIntent());
        payment.setSignatureVerified(Boolean.TRUE);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setFailureReason(null);
        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markPaymentSucceeded(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_CONFIRMED_FROM_RETURN", saved);
        return new ApiMessageResponse("Payment confirmed successfully");
    }

    @Transactional
    public void cancelConfirmedOrder(Long orderId, UserEntity user) {
        if (!redisStateService.tryAcquireCancelLock(orderId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cancellation is already being processed");
        }

        try {
        OrderEntity order = orderService.getOrderEntityForUser(orderId, user.getId());
        if (!"CONFIRMED".equals(order.getOrderStatus()) || !PaymentStatus.SUCCEEDED.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only paid confirmed orders can be cancelled");
        }

        PaymentEntity payment = paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment not found for order"));

        if (!"STRIPE".equals(payment.getProvider())) {
            throw new ResponseStatusException(BAD_REQUEST, "Only Stripe payments can be cancelled in this demo");
        }
        if (payment.getRefundId() != null && !payment.getRefundId().isBlank()) {
            return;
        }
        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Payment is not ready for refund yet");
        }

        Refund refund = stripeService.refundPayment(payment.getProviderPaymentId(), UUID.randomUUID().toString());
        payment.setRefundId(refund.getId());
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        payment.setFailureReason(null);
        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markCancelled(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_REFUNDED", saved);
        } finally {
            redisStateService.releaseCancelLock(orderId);
        }
    }

    @Transactional
    public void processStripeWebhook(String payload, String signatureHeader) {
        Event event = stripeService.verifyAndConstructEvent(payload, signatureHeader);
        if (!redisStateService.tryAcquireWebhookEventLock(event.getId())) {
            return;
        }
        if (paymentRepository.findByProviderEventId(event.getId()).isPresent()) {
            return;
        }

        StripeObject stripeObject = stripeService.deserializeEventObject(event);
        try {
            switch (event.getType()) {
                case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> {
                    if (stripeObject instanceof Session session) {
                        markWebhookPaymentSucceeded(event.getId(), resolvePaymentFromSession(session), session.getPaymentIntent());
                    }
                }
                case "checkout.session.async_payment_failed", "checkout.session.expired" -> {
                    if (stripeObject instanceof Session session) {
                        markWebhookPaymentFailed(event.getId(), resolvePaymentFromSession(session), event.getType());
                    }
                }
                case "payment_intent.succeeded" -> {
                    if (stripeObject instanceof PaymentIntent paymentIntent) {
                        markWebhookPaymentSucceeded(event.getId(), resolvePaymentFromPaymentIntent(paymentIntent), paymentIntent.getId());
                    }
                }
                case "payment_intent.payment_failed", "payment_intent.canceled" -> {
                    if (stripeObject instanceof PaymentIntent paymentIntent) {
                        markWebhookPaymentFailed(event.getId(), resolvePaymentFromPaymentIntent(paymentIntent), event.getType());
                    }
                }
                case "charge.refunded" -> {
                    if (stripeObject instanceof com.stripe.model.Charge charge) {
                        markWebhookPaymentRefunded(event.getId(), resolvePaymentFromPaymentIntentId(charge.getPaymentIntent()), charge.getRefunds() != null
                                && charge.getRefunds().getData() != null
                                && !charge.getRefunds().getData().isEmpty()
                                ? charge.getRefunds().getData().get(0).getId()
                                : null);
                    }
                }
                default -> {
                }
            }
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == NOT_FOUND.value()) {
                return;
            }
            throw ex;
        } catch (NumberFormatException ex) {
            return;
        }
    }

    private PaymentResponse createNewPayment(CreatePaymentRequest request, UserEntity user, String provider) {
        OrderEntity order = orderService.getOrderEntityForUser(request.getOrderId(), user.getId());
        if (!PaymentStatus.PENDING.equals(order.getPaymentStatus()) && !PaymentStatus.FAILED.equals(order.getPaymentStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Order is not in a payable state");
        }

        PaymentEntity payment = new PaymentEntity();
        LocalDateTime now = LocalDateTime.now();
        payment.setOrderId(order.getId());
        payment.setProvider(provider);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setIdempotencyKey(request.getIdempotencyKey());
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setSignatureVerified(Boolean.FALSE);

        String checkoutUrl = null;
        if ("STRIPE".equals(provider)) {
            Session session = stripeService.createCheckoutSession(order, request.getIdempotencyKey());
            payment.setSessionId(session.getId());
            payment.setStatus(PaymentStatus.REQUIRES_ACTION);
            checkoutUrl = session.getUrl();
        } else {
            payment.setStatus(PaymentStatus.PENDING);
            payment.setFailureReason("Razorpay flow placeholder - provider accepted but not yet integrated");
        }

        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markPaymentPending(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_CREATED", saved);
        return map(saved, checkoutUrl);
    }

    private PaymentResponse map(PaymentEntity payment, String checkoutUrl) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .provider(payment.getProvider())
                .providerPaymentId(payment.getProviderPaymentId())
                .refundId(payment.getRefundId())
                .providerEventId(payment.getProviderEventId())
                .sessionId(payment.getSessionId())
                .status(payment.getStatus())
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .idempotencyKey(payment.getIdempotencyKey())
                .signatureVerified(payment.getSignatureVerified())
                .failureReason(payment.getFailureReason())
                .checkoutUrl(checkoutUrl)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private PaymentEntity resolvePaymentFromSession(Session session) {
        return paymentRepository.findBySessionId(session.getId())
                .orElseGet(() -> resolvePaymentFromPaymentIntentId(session.getPaymentIntent()));
    }

    private PaymentEntity resolvePaymentFromPaymentIntent(PaymentIntent paymentIntent) {
        return resolvePaymentFromPaymentIntentId(paymentIntent.getId());
    }

    private PaymentEntity resolvePaymentFromPaymentIntentId(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new ResponseStatusException(NOT_FOUND, "Stripe payment intent id not found");
        }

        return paymentRepository.findFirstByProviderPaymentIdOrderByCreatedAtDesc(paymentIntentId)
                .orElseGet(() -> {
                    PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);
                    String orderId = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("orderId") : null;
                    if (orderId == null || orderId.isBlank()) {
                        throw new ResponseStatusException(NOT_FOUND, "Payment not found for Stripe payment intent");
                    }

                    Long resolvedOrderId = Long.valueOf(orderId);
                    return paymentRepository.findFirstByOrderIdOrderByCreatedAtDesc(resolvedOrderId)
                            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Payment not found for order"));
                });
    }

    private void markWebhookPaymentSucceeded(String eventId, PaymentEntity payment, String providerPaymentId) {
        OrderEntity order = orderService.getOrderEntity(payment.getOrderId());
        if (PaymentStatus.REFUNDED.equals(payment.getStatus())) {
            return;
        }

        payment.setProviderEventId(eventId);
        if (providerPaymentId != null && !providerPaymentId.isBlank()) {
            payment.setProviderPaymentId(providerPaymentId);
        }
        payment.setSignatureVerified(Boolean.TRUE);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setFailureReason(null);
        payment.setUpdatedAt(LocalDateTime.now());
        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markPaymentSucceeded(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_SUCCEEDED", saved);
    }

    private void markWebhookPaymentFailed(String eventId, PaymentEntity payment, String failureReason) {
        OrderEntity order = orderService.getOrderEntity(payment.getOrderId());
        if (PaymentStatus.SUCCEEDED.equals(payment.getStatus()) || PaymentStatus.REFUNDED.equals(payment.getStatus())) {
            return;
        }

        payment.setProviderEventId(eventId);
        payment.setSignatureVerified(Boolean.TRUE);
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        payment.setUpdatedAt(LocalDateTime.now());
        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markPaymentFailed(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_FAILED", saved);
    }

    private void markWebhookPaymentRefunded(String eventId, PaymentEntity payment, String refundId) {
        OrderEntity order = orderService.getOrderEntity(payment.getOrderId());
        payment.setProviderEventId(eventId);
        payment.setSignatureVerified(Boolean.TRUE);
        payment.setStatus(PaymentStatus.REFUNDED);
        if (refundId != null && !refundId.isBlank()) {
            payment.setRefundId(refundId);
        }
        payment.setFailureReason(null);
        payment.setUpdatedAt(LocalDateTime.now());
        PaymentEntity saved = paymentRepository.save(payment);
        orderService.markCancelled(order);
        paymentEventPublisher.publishPaymentEvent("PAYMENT_REFUNDED_FROM_WEBHOOK", saved);
    }
}
