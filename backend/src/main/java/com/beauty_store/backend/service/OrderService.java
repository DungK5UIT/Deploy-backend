package com.beauty_store.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.beauty_store.backend.model.CartItem;
import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.model.OrderItem;
import com.beauty_store.backend.repository.CartItemRepository;
import com.beauty_store.backend.repository.OrderItemRepository;
import com.beauty_store.backend.repository.OrderRepository;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Transactional
    public Order createOrder(Long userId, String paymentMethod, String shippingAddress, String note) {
        // Validate inputs
        if (userId == null) {
            logger.error("User ID is null");
            throw new IllegalArgumentException("User ID is required");
        }
        if (!List.of("VNPAY", "CASH_ON_DELIVERY", "MOMO", "OTHER").contains(paymentMethod.toUpperCase())) {
            logger.error("Invalid payment method: {}", paymentMethod);
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
        }
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            logger.error("Shipping address is empty");
            throw new IllegalArgumentException("Shipping address is required");
        }

        // Lấy cart items của user
        List<CartItem> cartItems = cartItemRepository.findByCartUserId(userId);
        if (cartItems.isEmpty()) {
            logger.error("Cart is empty for user: {}", userId);
            throw new IllegalArgumentException("Cart is empty");
        }

        // Tính total_amount từ cart items
        BigDecimal totalAmount = cartItems.stream()
                .map(item -> {
                    if (item.getPrice() == null || item.getQuantity() <= 0) {
                        logger.error("Invalid cart item: price={}, quantity={}", item.getPrice(), item.getQuantity());
                        throw new IllegalArgumentException("Invalid cart item: price or quantity is invalid");
                    }
                    return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.error("Total amount is invalid: {}", totalAmount);
            throw new IllegalArgumentException("Total amount must be positive");
        }

        // Tạo order
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPaymentMethod(paymentMethod.toUpperCase());
        order.setShippingAddress(shippingAddress);
        order.setNote(note != null ? note : "Không có ghi chú");
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Lưu order
        order = orderRepository.save(order);
        logger.info("Created order with ID: {}, total_amount: {}", order.getId(), totalAmount);

        // Tạo order items từ cart items
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItemRepository.save(orderItem);
        }

        // Xóa cart items sau khi tạo order
        cartItemRepository.deleteByCartUserId(userId);

        return order;
    }
    
}