package com.beauty_store.backend.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.beauty_store.backend.model.Order;
import com.beauty_store.backend.model.User;
import com.beauty_store.backend.service.OrderService;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Value("${vnpay.payment-url}")
    private String vnpPayUrl;

    @Value("${vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${vnpay.version}")
    private String vnpVersion;

    @Value("${vnpay.command}")
    private String vnpCommand;

    @Value("${vnpay.curr-code}")
    private String vnpCurrCode;

    @Value("${vnpay.locale}")
    private String vnpLocale;

    @Value("${vnpay.order-type}")
    private String vnpOrderType;

    @Autowired
    private OrderService orderService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody Map<String, Object> request) throws Exception {
        // Lấy user từ JWT (SecurityContext)
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        String paymentMethod = "vnpay";
        @SuppressWarnings("unchecked")
        Map<String, String> shippingInfo = (Map<String, String>) request.get("shippingInfo");

        // Tạo order từ cart
        Order order = orderService.createOrderFromCart(user, paymentMethod, shippingInfo);

        // Params từ request
        long amount = (long) (order.getTotalAmount() * 100); // VNPay yêu cầu amount * 100
        String orderInfo = (String) request.getOrDefault("orderInfo", "Thanh toan don hang " + order.getId());
        String orderId = order.getId().toString();
        String ipAddr = (String) request.getOrDefault("ipAddr", "127.0.0.1");

        // Build VNPay params (sắp xếp alphabet để hash đúng)
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", vnpCurrCode);
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", orderInfo);
        vnpParams.put("vnp_OrderType", vnpOrderType);
        vnpParams.put("vnp_Locale", vnpLocale);
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);
        vnpParams.put("vnp_IpAddr", ipAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Build query
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII.toString()));
            query.append("=");
            query.append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII.toString()));
            query.append("&");
        }
        String queryUrl = query.substring(0, query.length() - 1);

        // Tính secure hash
        String signData = queryUrl;
        String vnpSecureHash = hmacSHA512(vnpHashSecret, signData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

        String paymentUrl = vnpPayUrl + "?" + queryUrl;

        Map<String, String> response = new HashMap<>();
        response.put("paymentUrl", paymentUrl);

        return ResponseEntity.ok(response);
    }

    private static String hmacSHA512(final String key, final String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        return hash.toString();
    }
}