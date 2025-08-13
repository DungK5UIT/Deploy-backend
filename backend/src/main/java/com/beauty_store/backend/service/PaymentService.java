package com.beauty_store.backend.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.beauty_store.backend.dto.OrderRequest;
import com.beauty_store.backend.response.PaymentResponse;

@Service
public class PaymentService {

    private final String vnp_TmnCode = "UD8U2RBO";
    private final String vnp_HashSecret = "NYPFW7U8TVVBC712NIOCEHBEHF3QGK1A";
    private final String vnp_Url = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private final String vnp_ReturnUrl = "https://beauty-store-rho.vercel.app/payment-callback";

    public PaymentResponse createVNPayPayment(OrderRequest orderRequest) throws UnsupportedEncodingException {
        String orderId = "ORDER_" + System.currentTimeMillis();
        long amount = orderRequest.getAmount() * 100; // VNPay yêu cầu số tiền nhân 100
        String orderInfo = orderRequest.getOrderInfo();
        String ipAddr = orderRequest.getIpAddr();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderId);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "250000"); // Loại đơn hàng (có thể điều chỉnh)
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", ipAddr);
        vnp_Params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));

        // Sắp xếp tham số và tạo chữ ký bảo mật
        StringBuilder query = new StringBuilder();
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8.toString()))
                     .append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8.toString()))
                     .append('&');
                hashData.append(fieldName)
                        .append('=')
                        .append(fieldValue)
                        .append('&');
            }
        }
        query.deleteCharAt(query.length() - 1);
        hashData.deleteCharAt(hashData.length() - 1);

        String vnp_SecureHash = hmacSHA512(vnp_HashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(vnp_SecureHash);

        String paymentUrl = vnp_Url + "?" + query.toString();
        return new PaymentResponse(paymentUrl, "Tạo URL thanh toán VNPay thành công");
    }

    private String hmacSHA512(String key, String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] hmacData = md.digest((key + data).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacData) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi tạo HMAC SHA512", e);
        }
    }
}