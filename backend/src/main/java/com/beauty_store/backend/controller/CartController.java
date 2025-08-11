package com.beauty_store.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.model.CartItem;
import com.beauty_store.backend.model.ErrorResponse;
import com.beauty_store.backend.model.Product;
import com.beauty_store.backend.repository.ProductRepository;
import com.beauty_store.backend.service.CartService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final Logger logger = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/{userId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<?> getCartItems(@PathVariable Long userId) {
        try {
            logger.info("Fetching cart items for user {}", userId);
            List<CartItem> cartItems = cartService.getCartItems(userId);
            return ResponseEntity.ok(cartItems);
        } catch (Exception e) {
            logger.error("Error fetching cart items for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Không thể lấy danh sách giỏ hàng: " + e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    @PostMapping("/add/{userId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<?> addToCart(@PathVariable Long userId, @Valid @RequestBody CartItemRequest request) {
        try {
            logger.info("Adding product {} to cart for user {}", request.getProductId(), userId);
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));
            CartItem cartItem = cartService.addOrUpdateCartItem(userId, request.getProductId(), request.getQuantity());
            return ResponseEntity.ok(cartItem);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid data for adding to cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Dữ liệu không hợp lệ: " + e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error adding to cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @PostMapping("/update/{userId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<?> updateCartItem(@PathVariable Long userId, @Valid @RequestBody CartItemUpdateRequest request) {
        try {
            logger.info("Updating cart item {} for user {}", request.getCartItemId(), userId);
            CartItem cartItem = cartService.updateCartItem(userId, request.getCartItemId(), request.getQuantity());
            return ResponseEntity.ok(cartItem);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid data for updating cart: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Dữ liệu không hợp lệ: " + e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error updating cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @DeleteMapping("/remove/{userId}/{cartItemId}")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<?> removeFromCart(@PathVariable Long userId, @PathVariable Long cartItemId) {
        try {
            logger.info("Removing cart item {} for user {}", cartItemId, userId);
            cartService.removeFromCart(userId, cartItemId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error removing cart item: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Không thể xóa sản phẩm khỏi giỏ hàng: " + e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        }
    }

    static class CartItemRequest {
        @NotNull(message = "Product ID không được để trống")
        private Long productId;

        @Positive(message = "Quantity phải lớn hơn 0")
        private int quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    static class CartItemUpdateRequest {
        @NotNull(message = "CartItem ID không được để trống")
        private Long cartItemId;

        @Positive(message = "Quantity phải lớn hơn 0")
        private int quantity;

        public Long getCartItemId() {
            return cartItemId;
        }

        public void setCartItemId(Long cartItemId) {
            this.cartItemId = cartItemId;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}