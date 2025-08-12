package com.beauty_store.backend.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtil {
    public static String hashAllFields(Map<String, String> fields, String hashSecret) throws Exception {
        TreeMap<String, String> sortedFields = new TreeMap<>(fields);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedFields.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append(entry.getKey())
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                  .append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1); // Xóa dấu & cuối
        return hmacSHA512(hashSecret, sb.toString());
    }

    public static String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        mac.init(secretKey);
        byte[] result = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String getIpAddress(String remoteAddr) {
        if (remoteAddr != null && remoteAddr.startsWith("0:0:0:0:0:0:0:1")) {
            return "127.0.0.1";
        }
        return remoteAddr != null ? remoteAddr : "127.0.0.1";
    }
}