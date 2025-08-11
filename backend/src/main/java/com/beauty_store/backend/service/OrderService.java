package com.beauty_store.backend.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private static final List<String> VALID_PAYMENT_METHODS = Arrays.asList("VNPAY", "CASH");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Transactional
    public Order createOrder(Long userId, String paymentMethod, String shippingAddress, String note) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required");
        }
        if (!VALID_PAYMENT_METHODS.contains(paymentMethod.toUpperCase())) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethod);
        }
        if (shippingAddress == null || shippingAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("Shipping address is required");
        }

        List<CartItem> cartItems = cartItemRepository.findByCartUserId(userId);
        if (cartItems.isEmpty()) {
            logger.warn("Giỏ hàng trống cho user ID: {}", userId);
            throw new IllegalArgumentException("Giỏ hàng trống");
        }

        BigDecimal totalAmount = cartItems.stream()
                .map(item -> item.getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setPaymentMethod(paymentMethod.toUpperCase());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setShippingAddress(shippingAddress);
        order.setNote(note);
        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(cartItem.getProduct().getId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItemRepository.save(orderItem);
        }

        cartItemRepository.deleteByCartUserId(userId);

        logger.info("Tạo đơn hàng thành công: ID = {}, User ID = {}, Total Amount = {}", order.getId(), userId, totalAmount);
        return order;
    }
}