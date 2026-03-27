package com.javatechie.service;

import com.javatechie.model.OrderEntity;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Refund;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class StripeService {

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.webhookSecret:}")
    private String webhookSecret;

    @Value("${app.base-url:http://localhost:8081}")
    private String baseUrl;

    public Session createCheckoutSession(OrderEntity order, String idempotencyKey) {
        Stripe.apiKey = secretKey;

        SessionCreateParams.LineItem.PriceData.ProductData productData =
                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(order.getProductName())
                        .build();

        SessionCreateParams.LineItem.PriceData priceData =
                SessionCreateParams.LineItem.PriceData.builder()
                        .setCurrency(order.getCurrency().toLowerCase())
                        .setUnitAmount(order.getUnitAmount())
                        .setProductData(productData)
                        .build();

        SessionCreateParams.LineItem lineItem =
                SessionCreateParams.LineItem.builder()
                        .setQuantity((long) order.getQuantity())
                        .setPriceData(priceData)
                        .build();

        SessionCreateParams.PaymentIntentData paymentIntentData =
                SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("orderId", String.valueOf(order.getId()))
                        .putMetadata("userId", String.valueOf(order.getUserId()))
                        .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/cancel?order_id=" + order.getId())
                .setClientReferenceId(String.valueOf(order.getId()))
                .putMetadata("orderId", String.valueOf(order.getId()))
                .putMetadata("userId", String.valueOf(order.getUserId()))
                .setPaymentIntentData(paymentIntentData)
                .addLineItem(lineItem)
                .build();

        try {
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();
            return Session.create(params, options);
        } catch (StripeException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe session creation failed: " + ex.getMessage());
        }
    }

    public Event verifyAndConstructEvent(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Stripe webhook secret is not configured");
        }

        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid Stripe signature");
        }
    }

    public Refund refundPayment(String paymentIntentId, String idempotencyKey) {
        Stripe.apiKey = secretKey;

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(paymentIntentId)
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            return Refund.create(params, options);
        } catch (StripeException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Stripe refund failed: " + ex.getMessage());
        }
    }

    public Session retrieveCheckoutSession(String sessionId) {
        Stripe.apiKey = secretKey;

        try {
            return Session.retrieve(sessionId);
        } catch (StripeException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unable to retrieve Stripe session: " + ex.getMessage());
        }
    }

    public PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        Stripe.apiKey = secretKey;

        try {
            return PaymentIntent.retrieve(paymentIntentId);
        } catch (StripeException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Unable to retrieve Stripe payment intent: " + ex.getMessage());
        }
    }

    public StripeObject deserializeEventObject(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            return deserializer.getObject().get();
        }

        try {
            return deserializer.deserializeUnsafe();
        } catch (Exception ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to deserialize Stripe event object");
        }
    }
}
