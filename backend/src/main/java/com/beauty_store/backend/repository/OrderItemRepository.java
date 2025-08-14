package com.beauty_store.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beauty_store.backend.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
}