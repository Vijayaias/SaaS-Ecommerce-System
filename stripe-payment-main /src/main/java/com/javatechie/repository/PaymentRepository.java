package com.javatechie.repository;

import com.javatechie.model.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentEntity> findBySessionId(String sessionId);
    Optional<PaymentEntity> findByProviderEventId(String providerEventId);
    Optional<PaymentEntity> findFirstByProviderPaymentIdOrderByCreatedAtDesc(String providerPaymentId);
    List<PaymentEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<PaymentEntity> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}
