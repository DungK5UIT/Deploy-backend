package com.beauty_store.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.dto.OrderRequest;
import com.beauty_store.backend.response.PaymentResponse;
import com.beauty_store.backend.service.PaymentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody OrderRequest orderRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body(new PaymentResponse(null, "Vui lòng đăng nhập để thanh toán"));
        }

        try {
            String paymentMethod = orderRequest.getPaymentMethod();
            if ("vnpay".equalsIgnoreCase(paymentMethod)) {
                PaymentResponse response = paymentService.createVNPayPayment(orderRequest);
                return ResponseEntity.ok(response);
            } else if ("cod".equalsIgnoreCase(paymentMethod)) {
                return ResponseEntity.ok(new PaymentResponse(null, "Đặt hàng COD thành công"));
            } else {
                return ResponseEntity.badRequest().body(new PaymentResponse(null, "Phương thức thanh toán không hợp lệ"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new PaymentResponse(null, "Lỗi xử lý thanh toán: " + e.getMessage()));
        }
    }
}