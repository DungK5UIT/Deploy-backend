package com.beauty_store.backend.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.model.ErrorResponse;
import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.model.OrderItem;
import com.beauty_store.backend.repository.OrderItemRepository;
import com.beauty_store.backend.repository.OrderRepository;
import com.beauty_store.backend.service.OrderService;
import com.beauty_store.backend.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> createOrder(
            @Valid @RequestBody OrderRequest orderRequest,
            HttpServletRequest request) {
        try {
            logger.info("Creating order for user: {}", orderRequest.getUserId());
            
            String paymentMethod = orderRequest.getPaymentMethod().toUpperCase();
            
            // Adjust payment method for COD to match service validation
            if ("COD".equals(paymentMethod)) {
                orderRequest.setPaymentMethod("CASH_ON_DELIVERY");
            }
            
            Order order = orderService.createOrder(
                    orderRequest.getUserId(),
                    orderRequest.getPaymentMethod(),
                    orderRequest.getShippingAddress(),
                    orderRequest.getNote()
            );

            if ("VNPAY".equals(paymentMethod)) {
                String ipAddress = request.getRemoteAddr();
                String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
                return ResponseEntity.ok(new PaymentResponse(order.getId(), paymentUrl));
            } else {
                // For COD or other methods, return the order directly
                return ResponseEntity.ok(order);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Error creating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Unexpected error creating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi hệ thống: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> getOrdersByUser(@PathVariable Long userId, Authentication authentication) {
        try {
            // Nếu username là ID, bạn có thể so sánh ở đây
            String loggedInUserId = authentication.getName();
            if (!loggedInUserId.equals(String.valueOf(userId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("Bạn không được phép truy cập đơn hàng của người khác", HttpStatus.FORBIDDEN.value()));
            }

            List<Order> orders = orderRepository.findByUserId(userId);
            for (Order order : orders) {
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
                order.setItems(items); // @Transient trong model Order
            }
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error fetching orders: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Error fetching orders", HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }
}

// DTO Request
class OrderRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String note;

    // Getters & Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

// DTO Response cho VNPay
class PaymentResponse {
    private Long orderId;
    private String paymentUrl;

    public PaymentResponse(Long orderId, String paymentUrl) {
        this.orderId = orderId;
        this.paymentUrl = paymentUrl;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
}