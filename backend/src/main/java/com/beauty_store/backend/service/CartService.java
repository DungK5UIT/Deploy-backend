package com.beauty_store.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.beauty_store.backend.model.Cart;
import com.beauty_store.backend.model.CartItem;
import com.beauty_store.backend.model.Product;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.repository.CartItemRepository;
import com.beauty_store.backend.repository.CartRepository;
import com.beauty_store.backend.repository.ProductRepository;
import com.beauty_store.backend.repository.UserRepository;

@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    public Cart getCart(Long userId) {
        logger.debug("Lấy giỏ hàng cho user ID: {}", userId);
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    logger.info("Tạo giỏ hàng mới cho user ID: {}", userId);
                    return cartRepository.save(newCart);
                });
    }

    public CartItem addOrUpdateCartItem(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = getCart(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        CartItem existingItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            logger.info("Cập nhật số lượng CartItem: Cart ID = {}, Product ID = {}, New Quantity = {}", 
                        cart.getId(), productId, existingItem.getQuantity());
            return cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setPrice(new BigDecimal(String.valueOf(product.getPrice()))); // Chuyển double thành BigDecimal
            logger.info("Thêm CartItem mới: Cart ID = {}, Product ID = {}, Quantity = {}", 
                        cart.getId(), productId, quantity);
            return cartItemRepository.save(newItem);
        }
    }

    public CartItem updateCartItem(Long userId, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        Cart cart = getCart(userId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));

        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user: " + userId);
        }

        cartItem.setQuantity(quantity);
        logger.info("Cập nhật CartItem: ID = {}, New Quantity = {}", cartItemId, quantity);
        return cartItemRepository.save(cartItem);
    }

    public List<CartItem> getCartItems(Long userId) {
        Cart cart = getCart(userId);
        logger.debug("Lấy danh sách CartItem cho Cart ID: {}", cart.getId());
        return cartItemRepository.findByCartId(cart.getId());
    }

    public void removeFromCart(Long userId, Long cartItemId) {
        Cart cart = getCart(userId);
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        if (!cartItem.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to user: " + userId);
        }
        cartItemRepository.deleteById(cartItemId);
        logger.info("Xóa CartItem: ID = {}, User ID = {}", cartItemId, userId);
    }
}