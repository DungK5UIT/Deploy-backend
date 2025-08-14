package com.beauty_store.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.beauty_store.backend.model.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    
}