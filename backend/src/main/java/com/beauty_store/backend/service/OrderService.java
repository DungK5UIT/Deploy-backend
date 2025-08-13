package com.beauty_store.backend.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.beauty_store.backend.model.Cart;
import com.beauty_store.backend.model.CartItem;
import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.model.OrderItem;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartService cartService;

    public Order createOrderFromCart(User user, String paymentMethod, Map<String, String> shippingInfo) {
        Cart cart = cartService.getCart(user.getId());
        if (cart == null) {
            throw new RuntimeException("Cart not found");
        }

        List<CartItem> cartItems = cartService.getCartItems(cart.getId());
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = new Order();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        order.setPaymentMethod(paymentMethod);
        order.setFullName(shippingInfo.get("fullName"));
        order.setPhone(shippingInfo.get("phone"));
        order.setEmail(shippingInfo.get("email"));
        order.setAddress(shippingInfo.get("address"));
        order.setCity(shippingInfo.get("city"));
        order.setDistrict(shippingInfo.get("district"));

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItems.add(orderItem);
        }
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Clear cart after creating order
        cartService.clearCart(cart);

        return savedOrder;
    }
}