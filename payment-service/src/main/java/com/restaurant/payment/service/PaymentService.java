package com.restaurant.payment.service;

import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.dto.PaymentRefundRequest;

public interface PaymentService {
    PaymentResponse processPaymentRequest(PaymentRequest request);
    void processRefundRequest(PaymentRefundRequest request);
    void handlePaymentIntentSucceeded(String paymentIntentId, long amount);
    void handlePaymentIntentFailed(String paymentIntentId, String errorMessage);
}
