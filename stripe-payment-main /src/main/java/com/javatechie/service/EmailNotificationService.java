package com.javatechie.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatechie.model.OrderEntity;
import com.javatechie.model.UserEntity;
import com.javatechie.repository.OrderRepository;
import com.javatechie.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RedisStateService redisStateService;
    private final EventAuditService eventAuditService;
    private final boolean mailEnabled;
    private final String fromAddress;

    public EmailNotificationService(JavaMailSender mailSender,
                                    ObjectMapper objectMapper,
                                    OrderRepository orderRepository,
                                    UserRepository userRepository,
                                    RedisStateService redisStateService,
                                    EventAuditService eventAuditService,
                                    @Value("${app.mail.enabled:false}") boolean mailEnabled,
                                    @Value("${app.mail.from:no-reply@checkoutstudio.local}") String fromAddress) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.redisStateService = redisStateService;
        this.eventAuditService = eventAuditService;
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
    }

    public void sendPaymentNotification(String payload) {
        if (!mailEnabled) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText("");
            if (!isNotificationEvent(eventType)) {
                return;
            }

            long orderId = root.path("orderId").asLong(-1L);
            if (orderId <= 0 || !redisStateService.tryAcquireNotificationLock(eventType, orderId)) {
                return;
            }

            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return;
            }

            UserEntity user = userRepository.findById(order.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }

            if (isReceiptEvent(eventType)) {
                sendReceiptEmail(user, order, eventType);
            } else if (isRefundEvent(eventType)) {
                sendRefundEmail(user, order, eventType);
            }
        } catch (Exception ex) {
            eventAuditService.record("mail-error", ex.getMessage());
        }
    }

    public void sendPaymentNotification(String eventType, Long orderId) {
        if (!mailEnabled || orderId == null || orderId <= 0) {
            return;
        }

        try {
            if (!isNotificationEvent(eventType) || !redisStateService.tryAcquireNotificationLock(eventType, orderId)) {
                return;
            }

            OrderEntity order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                return;
            }

            UserEntity user = userRepository.findById(order.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                return;
            }

            if (isReceiptEvent(eventType)) {
                sendReceiptEmail(user, order, eventType);
            } else if (isRefundEvent(eventType)) {
                sendRefundEmail(user, order, eventType);
            }
        } catch (Exception ex) {
            eventAuditService.record("mail-error", ex.getMessage());
        }
    }

    private boolean isNotificationEvent(String eventType) {
        return isReceiptEvent(eventType) || isRefundEvent(eventType);
    }

    private boolean isReceiptEvent(String eventType) {
        return "PAYMENT_SUCCEEDED".equals(eventType) || "PAYMENT_CONFIRMED_FROM_RETURN".equals(eventType);
    }

    private boolean isRefundEvent(String eventType) {
        return "PAYMENT_REFUNDED".equals(eventType) || "PAYMENT_REFUNDED_FROM_WEBHOOK".equals(eventType);
    }

    private void sendReceiptEmail(UserEntity user, OrderEntity order, String eventType) throws MessagingException, MailException {
        String subject = "Payment receipt for order #" + order.getId();
        String html = """
                <html>
                <body style="font-family:Arial,sans-serif;color:#24170f;">
                    <h2>Payment received successfully</h2>
                    <p>Hello %s,</p>
                    <p>Your payment for <strong>%s</strong> has been confirmed.</p>
                    <ul>
                        <li>Order ID: %d</li>
                        <li>Quantity: %d</li>
                        <li>Total: $%.2f</li>
                        <li>Status: %s / %s</li>
                    </ul>
                    <p>Thanks for shopping with Checkout Studio.</p>
                </body>
                </html>
                """.formatted(
                user.getUsername(),
                order.getProductName(),
                order.getId(),
                order.getQuantity(),
                order.getTotalAmount() / 100.0,
                order.getOrderStatus(),
                order.getPaymentStatus()
        );
        sendHtmlMail(user.getEmail(), subject, html);
        eventAuditService.record("mail", "Sent receipt email for " + eventType + " orderId=" + order.getId());
    }

    private void sendRefundEmail(UserEntity user, OrderEntity order, String eventType) throws MessagingException, MailException {
        String subject = "Refund initiated for order #" + order.getId();
        String html = """
                <html>
                <body style="font-family:Arial,sans-serif;color:#24170f;">
                    <h2>Refund initiated</h2>
                    <p>Hello %s,</p>
                    <p>Your order <strong>%s</strong> has been cancelled and a refund has been initiated.</p>
                    <ul>
                        <li>Order ID: %d</li>
                        <li>Total: $%.2f</li>
                        <li>Status: %s / %s</li>
                    </ul>
                    <p>If this was not expected, please review your recent activity.</p>
                </body>
                </html>
                """.formatted(
                user.getUsername(),
                order.getProductName(),
                order.getId(),
                order.getTotalAmount() / 100.0,
                order.getOrderStatus(),
                order.getPaymentStatus()
        );
        sendHtmlMail(user.getEmail(), subject, html);
        eventAuditService.record("mail", "Sent refund email for " + eventType + " orderId=" + order.getId());
    }

    private void sendHtmlMail(String to, String subject, String html) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(mimeMessage);
    }
}
