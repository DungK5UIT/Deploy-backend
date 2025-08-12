package com.beauty_store.backend.controller;

import com.beauty_store.backend.config.VNPayConfig;
import com.beauty_store.backend.util.VNPayUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private VNPayConfig vnPayConfig;

    @PostMapping("/create")
    public Map<String, String> createPayment(@RequestBody Map<String, Object> request) throws Exception {
        Map<String, String> response = new HashMap<>();
        String orderId = (String) request.get("orderId");
        String amount = String.valueOf(request.get("amount"));
        String orderInfo = (String) request.get("orderInfo");
        String ipAddr = (String) request.get("ipAddr");
        Map<String, String> shippingInfo = (Map<String, String>) request.get("shippingInfo");

        // Tạo tham số yêu cầu VNPay
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(Integer.parseInt(amount) * 100));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", "250000");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(ipAddr));
        vnpParams.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        vnpParams.put("vnp_ExpireDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(System.currentTimeMillis() + 15 * 60 * 1000)));

        // Thêm thông tin giao hàng vào OrderInfo để lưu trữ
        String shippingDetails = String.format("FullName: %s, Phone: %s, Email: %s, Address: %s, City: %s, District: %s",
                shippingInfo.get("fullName"), shippingInfo.get("phone"), shippingInfo.get("email"),
                shippingInfo.get("address"), shippingInfo.get("city"), shippingInfo.get("district"));
        vnpParams.put("vnp_OrderInfo", orderInfo + " | " + shippingDetails);

        // Tạo secure hash
        String secureHash = VNPayUtil.hashAllFields(vnpParams, vnPayConfig.getHashSecret());
        vnpParams.put("vnp_SecureHash", secureHash);

        // Tạo URL thanh toán
        StringBuilder paymentUrl = new StringBuilder(vnPayConfig.getPaymentUrl()).append("?");
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            paymentUrl.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                      .append("=")
                      .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                      .append("&");
        }
        paymentUrl.deleteCharAt(paymentUrl.length() - 1);

        response.put("paymentUrl", paymentUrl.toString());
        return response;
    }

    @GetMapping("/return")
    public Map<String, String> paymentReturn(@RequestParam Map<String, String> params) throws Exception {
        Map<String, String> response = new HashMap<>();
        String secureHash = params.remove("vnp_SecureHash");
        String calculatedHash = VNPayUtil.hashAllFields(params, vnPayConfig.getHashSecret());

        if (secureHash.equals(calculatedHash)) {
            if ("00".equals(params.get("vnp_TransactionStatus"))) {
                response.put("status", "success");
                response.put("message", "Giao dịch thành công");
                response.put("orderId", params.get("vnp_TxnRef"));
                response.put("amount", params.get("vnp_Amount"));
                response.put("transactionNo", params.get("vnp_TransactionNo"));
                // TODO: Lưu thông tin đơn hàng vào database tại đây
            } else {
                response.put("status", "failed");
                response.put("message", "Giao dịch thất bại");
            }
        } else {
            response.put("status", "failed");
            response.put("message", "Sai chữ ký, dữ liệu không hợp lệ");
        }
        return response;
    }
}