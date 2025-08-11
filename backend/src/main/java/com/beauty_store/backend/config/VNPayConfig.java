package com.beauty_store.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

@Configuration
public class VNPayConfig {

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    @Value("${vnpay.payment-url}")
    private String paymentUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Bean
    public VNPayProperties vnPayProperties() {
        return new VNPayProperties(tmnCode, hashSecret, paymentUrl, returnUrl);
    }

    public static class VNPayProperties {
        private final String tmnCode;
        private final String hashSecret;
        private final String paymentUrl;
        private final String returnUrl;

        public VNPayProperties(String tmnCode, String hashSecret, String paymentUrl, String returnUrl) {
            this.tmnCode = tmnCode;
            this.hashSecret = hashSecret;
            this.paymentUrl = paymentUrl;
            this.returnUrl = returnUrl;
        }

        public String getTmnCode() {
            return tmnCode;
        }

        public String getHashSecret() {
            return hashSecret;
        }

        public String getPaymentUrl() {
            return paymentUrl;
        }

        public String getReturnUrl() {
            return returnUrl;
        }
    }
}