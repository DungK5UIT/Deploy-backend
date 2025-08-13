package com.beauty_store.backend.response;

public class PaymentResponse {
    private String paymentUrl;
    private String message;

    public PaymentResponse(String paymentUrl, String message) {
        this.paymentUrl = paymentUrl;
        this.message = message;
    }

    // Getters and Setters
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}