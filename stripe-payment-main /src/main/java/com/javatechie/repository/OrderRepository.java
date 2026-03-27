package com.javatechie.repository;

import com.javatechie.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<OrderEntity> findByIdAndUserId(Long id, Long userId);
}
