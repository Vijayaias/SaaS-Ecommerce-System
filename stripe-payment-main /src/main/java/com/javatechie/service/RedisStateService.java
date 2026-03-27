package com.javatechie.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisStateService {

    private final StringRedisTemplate redisTemplate;
    private final boolean redisEnabled;
    private final Duration paymentIdempotencyTtl;
    private final Duration cancelLockTtl;
    private final Duration webhookLockTtl;
    private final Duration notificationLockTtl;

    public RedisStateService(StringRedisTemplate redisTemplate,
                             @Value("${app.redis.enabled:true}") boolean redisEnabled,
                             @Value("${app.redis.payment-idempotency-ttl-minutes:30}") long paymentIdempotencyTtlMinutes,
                             @Value("${app.redis.cancel-lock-ttl-seconds:120}") long cancelLockTtlSeconds,
                             @Value("${app.redis.webhook-lock-ttl-minutes:30}") long webhookLockTtlMinutes,
                             @Value("${app.redis.notification-lock-ttl-minutes:1440}") long notificationLockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.redisEnabled = redisEnabled;
        this.paymentIdempotencyTtl = Duration.ofMinutes(paymentIdempotencyTtlMinutes);
        this.cancelLockTtl = Duration.ofSeconds(cancelLockTtlSeconds);
        this.webhookLockTtl = Duration.ofMinutes(webhookLockTtlMinutes);
        this.notificationLockTtl = Duration.ofMinutes(notificationLockTtlMinutes);
    }

    public boolean tryReservePaymentIdempotency(String idempotencyKey) {
        return setIfAbsent("payment:idempotency:" + idempotencyKey, "IN_PROGRESS", paymentIdempotencyTtl);
    }

    public void markPaymentIdempotencyCompleted(String idempotencyKey, String paymentId) {
        setValue("payment:idempotency:" + idempotencyKey, paymentId, paymentIdempotencyTtl);
    }

    public void clearPaymentIdempotency(String idempotencyKey) {
        delete("payment:idempotency:" + idempotencyKey);
    }

    public boolean tryAcquireCancelLock(Long orderId) {
        return setIfAbsent("order:cancel:" + orderId, "LOCKED", cancelLockTtl);
    }

    public void releaseCancelLock(Long orderId) {
        delete("order:cancel:" + orderId);
    }

    public boolean tryAcquireWebhookEventLock(String eventId) {
        return setIfAbsent("stripe:webhook:event:" + eventId, "PROCESSING", webhookLockTtl);
    }

    public boolean tryAcquireNotificationLock(String notificationType, Long orderId) {
        return setIfAbsent("notification:" + notificationType + ":" + orderId, "SENT", notificationLockTtl);
    }

    private boolean setIfAbsent(String key, String value, Duration ttl) {
        if (!redisEnabled) {
            return true;
        }

        try {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return true;
        }
    }

    private void setValue(String key, String value, Duration ttl) {
        if (!redisEnabled) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception ignored) {
        }
    }

    private void delete(String key) {
        if (!redisEnabled) {
            return;
        }

        try {
            redisTemplate.delete(key);
        } catch (Exception ignored) {
        }
    }
}
