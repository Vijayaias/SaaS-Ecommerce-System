package com.javatechie.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final EventAuditService eventAuditService;
    private final EmailNotificationService emailNotificationService;

    public KafkaEventConsumer(EventAuditService eventAuditService,
                              EmailNotificationService emailNotificationService) {
        this.eventAuditService = eventAuditService;
        this.emailNotificationService = emailNotificationService;
    }

    @KafkaListener(topics = "${app.kafka.topics.order-events:order-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderEvent(String payload) {
        log.info("Received order event: {}", payload);
        eventAuditService.record("order-events", payload);
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-events:payment-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentEvent(String payload) {
        log.info("Received payment event: {}", payload);
        eventAuditService.record("payment-events", payload);
        emailNotificationService.sendPaymentNotification(payload);
    }
}
