package com.beauty_store.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.model.ErrorResponse;
import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.repository.OrderRepository;
import com.beauty_store.backend.service.VNPayService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/pay")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/vnpay/initiate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> initiateVNPayPayment(@RequestParam Long orderId, HttpServletRequest request) {
        try {
            logger.info("Initiating VNPay payment for order: {}", orderId);
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

            String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
            if (!order.getUserId().toString().equals(currentUserId)) {
                logger.warn("User {} does not have permission to initiate payment for order {}", currentUserId, orderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ErrorResponse("You do not have permission to initiate payment for this order", HttpStatus.FORBIDDEN.value()));
            }

            if (!"PENDING".equals(order.getStatus())) {
                logger.warn("Order {} is not in PENDING status, current status: {}", orderId, order.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Order is not in PENDING status", HttpStatus.BAD_REQUEST.value()));
            }

            String ipAddress = request.getRemoteAddr();
            String paymentUrl = vnPayService.createPaymentUrl(order, ipAddress);
            return ResponseEntity.ok(new PaymentResponse(orderId, paymentUrl));
        } catch (IllegalArgumentException e) {
            logger.warn("Error initiating payment: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error initiating payment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("System error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> handleVNPayCallback(@RequestParam Map<String, String> params) {
        logger.info("Received VNPay callback with params: {}", params);
        try {
            if (!vnPayService.verifyPaymentResponse(params)) {
                logger.warn("Invalid payment signature for params: {}", params);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Invalid payment signature", HttpStatus.BAD_REQUEST.value()));
            }

            vnPayService.processPaymentCallback(params);
            String orderId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            logger.info("Callback response code: {} for order: {}", responseCode, orderId);

            Map<String, String> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "00".equals(responseCode) ? "SUCCESS" : 
                                   responseCode.equals("07") ? "INVALID_FORMAT" : 
                                   responseCode.equals("11") ? "TIMEOUT" : "FAILED");
            response.put("message", getVNPayErrorMessage(responseCode));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error processing callback: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST.value()));
        } catch (Exception e) {
            logger.error("Error processing callback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("System error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    private String getVNPayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "00": return "Giao dịch thành công";
            case "07": return "Dữ liệu gửi sang không đúng định dạng";
            case "11": return "Giao dịch đã quá thời gian chờ thanh toán";
            case "24": return "Giao dịch bị hủy bởi người dùng";
            default: return "Giao dịch thất bại: Mã lỗi " + responseCode;
        }
    }
}