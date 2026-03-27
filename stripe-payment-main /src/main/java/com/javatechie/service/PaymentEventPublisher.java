package com.javatechie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatechie.model.OrderEntity;
import com.javatechie.model.PaymentEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean kafkaEnabled;
    private final String orderEventsTopic;
    private final String paymentEventsTopic;

    public PaymentEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${app.kafka.enabled:true}") boolean kafkaEnabled,
                                 @Value("${app.kafka.topics.order-events:order-events}") String orderEventsTopic,
                                 @Value("${app.kafka.topics.payment-events:payment-events}") String paymentEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.kafkaEnabled = kafkaEnabled;
        this.orderEventsTopic = orderEventsTopic;
        this.paymentEventsTopic = paymentEventsTopic;
    }

    public void publishOrderEvent(String eventType, OrderEntity order) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("orderId", order.getId());
        payload.put("userId", order.getUserId());
        payload.put("productName", order.getProductName());
        payload.put("quantity", order.getQuantity());
        payload.put("totalAmount", order.getTotalAmount());
        payload.put("currency", order.getCurrency());
        payload.put("orderStatus", order.getOrderStatus());
        payload.put("paymentStatus", order.getPaymentStatus());
        payload.put("occurredAt", LocalDateTime.now().toString());
        send(orderEventsTopic, "order-" + order.getId(), payload);
    }

    public void publishPaymentEvent(String eventType, PaymentEntity payment) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("paymentId", payment.getId());
        payload.put("orderId", payment.getOrderId());
        payload.put("provider", payment.getProvider());
        payload.put("status", payment.getStatus());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("sessionId", payment.getSessionId());
        payload.put("providerPaymentId", payment.getProviderPaymentId());
        payload.put("refundId", payment.getRefundId());
        payload.put("providerEventId", payment.getProviderEventId());
        payload.put("occurredAt", LocalDateTime.now().toString());
        send(paymentEventsTopic, "payment-" + payment.getId(), payload);
    }

    private void send(String topic, String key, Map<String, Object> payload) {
        if (!kafkaEnabled) {
            return;
        }

        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ignored) {
        }
    }
}
