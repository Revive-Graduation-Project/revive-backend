package com.restaurant.payment.service;

import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@Slf4j
@Service
public class StripeService {

    public PaymentResponse createPaymentIntent(PaymentRequest request) throws StripeException {
        log.info("Creating PaymentIntent for order: {} with amount: {}", request.getOrderId(), request.getAmount());

        // Convert BigDecimal to cents for Stripe
        long amountInCents = request.getAmount().multiply(new BigDecimal(100)).longValue();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", String.valueOf(request.getOrderId()));
        metadata.put("clientId", String.valueOf(request.getClientId()));

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency())
                .setReceiptEmail(request.getReceiptEmail())
                .putAllMetadata(metadata)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent intent = PaymentIntent.create(params);

        return PaymentResponse.builder()
                .clientSecret(intent.getClientSecret())
                .paymentIntentId(intent.getId())
                .status(intent.getStatus())
                .build();
    }

    public void refundPayment(String paymentIntentId) throws StripeException {
        log.info("Refunding payment for intent: {}", paymentIntentId);
        RefundCreateParams params = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId)
                .build();
        Refund.create(params);
    }
}
