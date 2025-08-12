package com.beauty_store.backend.service;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.beauty_store.backend.config.VNPayConfig;
import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.model.Payment;
import com.beauty_store.backend.repository.OrderRepository;
import com.beauty_store.backend.repository.PaymentRepository;

@Service
public class VNPayService {
    private static final Logger logger = LoggerFactory.getLogger(VNPayService.class);

    @Autowired
    private VNPayConfig.VNPayProperties vnPayProperties;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    public String createPaymentUrl(Order order, String ipAddress) throws UnsupportedEncodingException {
        if (order == null || order.getId() == null) {
            throw new IllegalArgumentException("Order cannot be null and must have an ID");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order amount must be positive and non-null");
        }

        long amount = order.getTotalAmount().multiply(new BigDecimal("100")).longValue();
        
        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayProperties.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", String.valueOf(order.getId()));
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getId());
        vnp_Params.put("vnp_OrderType", "250000"); // Mã loại hàng hóa, bạn có thể thay đổi
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayProperties.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress);

        // Đảm bảo múi giờ GMT+7 cho VNPay
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15); // Timeout 15 phút
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        
        // SỬA LỖI 1: Build query string đúng cách (chỉ encode value)
        String queryString = vnp_Params.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        // This should never happen with UTF-8
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));
        
        String secureHash = generateSecureHash(queryString);
        String paymentUrl = vnPayProperties.getPaymentUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
        
        logger.info("Generated VNPay payment URL: {}", paymentUrl);
        
        // Save payment record
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentMethod("VNPAY");
        payment.setStatus("PENDING");
        // Lưu vnp_TxnRef để đối chiếu khi callback
        payment.setTransactionId(String.valueOf(order.getId()));
        payment.setCreatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        return paymentUrl;
    }

    public boolean verifyPaymentResponse(Map<String, String> params) {
        // SỬA LỖI 2: Loại bỏ vnp_SecureHash và vnp_SecureHashType trước khi hash
        String secureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType"); // Nếu có
        
        // Sắp xếp lại các tham số theo thứ tự alphabet và build query string
        Map<String, String> sortedParams = new TreeMap<>(params);
        String queryString = sortedParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                .map(entry -> {
                    try {
                        return entry.getKey() + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));

        String calculatedHash = generateSecureHash(queryString);
        logger.info("Verifying payment response: vnp_SecureHash={}, calculatedHash={}", secureHash, calculatedHash);
        return secureHash.equals(calculatedHash);
    }

    public void processPaymentCallback(Map<String, String> params) {
        String orderIdStr = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo"); // Mã giao dịch của VNPay

        logger.info("Processing VNPay callback: vnp_TxnRef={}, vnp_ResponseCode={}, vnp_TransactionNo={}", 
            orderIdStr, responseCode, transactionNo);

        Order order = orderRepository.findById(Long.parseLong(orderIdStr))
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderIdStr));

        // Chỉ xử lý nếu đơn hàng đang ở trạng thái PENDING
        if (!"PENDING".equals(order.getStatus())) {
            logger.warn("Order {} is not in PENDING status, skipping callback processing. Current status: {}", orderIdStr, order.getStatus());
            return;
        }

        // Tìm payment record bằng orderId và transactionId (chính là orderId lúc tạo)
        Payment payment = paymentRepository.findByOrderIdAndTransactionId(order.getId(), orderIdStr)
                .orElseThrow(() -> new IllegalArgumentException("Payment record not found for order: " + orderIdStr));
        
        payment.setResponseCode(responseCode);
        payment.setResponseData(new HashMap<>(params)); // Lưu toàn bộ data trả về từ VNPay
        
        if ("00".equals(responseCode)) {
            logger.info("Payment successful for order {}", orderIdStr);
            order.setStatus("PAID");
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            // Cập nhật transactionId bằng mã giao dịch thật của VNPay
            payment.setTransactionId(transactionNo);
        } else {
            logger.warn("Payment failed for order {}. Response code: {}", orderIdStr, responseCode);
            order.setStatus("FAILED");
            payment.setStatus("FAILED");
        }
        
        orderRepository.save(order);
        paymentRepository.save(payment);
        logger.info("Processed VNPay callback for order {}. New status: {}", orderIdStr, payment.getStatus());
    }

    private String generateSecureHash(String data) {
        try {
            Mac sha512_HMAC = Mac.getInstance("HmacSHA512");
            SecretKeySpec secret_key = new SecretKeySpec(vnPayProperties.getHashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            sha512_HMAC.init(secret_key);
            byte[] hash = sha512_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Error generating secure hash: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to generate secure hash", e);
        }
    }
}