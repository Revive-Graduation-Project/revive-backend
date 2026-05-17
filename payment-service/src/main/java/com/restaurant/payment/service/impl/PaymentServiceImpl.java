package com.restaurant.payment.service.impl;

import com.restaurant.payment.dto.PaymentFailedEvent;
import com.restaurant.payment.dto.PaymentRefundRequest;
import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.dto.PaymentSucceededEvent;
import com.restaurant.payment.entity.Payment;
import com.restaurant.payment.messaging.MessagePublisher;
import com.restaurant.payment.repository.PaymentRepository;
import com.restaurant.payment.service.PaymentService;
import com.restaurant.payment.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final StripeService stripeService;
    private final PaymentRepository paymentRepository;
    private final MessagePublisher messagePublisher;

    @Override
    @Transactional
    public PaymentResponse processPaymentRequest(PaymentRequest request) {
        log.info("Processing payment request for order: {}", request.getOrderId());

        Payment payment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(Payment.builder()
                        .orderId(request.getOrderId())
                        .amount(request.getAmount().multiply(new BigDecimal(100)).longValue()) // store in cents/piastres
                        .currency(request.getCurrency())
                        .status("PENDING")
                        .build());
        paymentRepository.save(payment);

        try {
            var stripeResponse = stripeService.createPaymentIntent(request);
            
            payment.setPaymentIntentId(stripeResponse.getPaymentIntentId());
            payment.setStripeStatus(stripeResponse.getStatus());
            paymentRepository.save(payment);

            return stripeResponse;

        } catch (StripeException e) {
            log.error("Stripe error for order {}: {}", request.getOrderId(), e.getMessage());
            payment.setStatus("FAILED");
            payment.setErrorMessage(e.getMessage());
            paymentRepository.save(payment);
            
            throw new RuntimeException("Payment processing failed", e);
        }
    }

    @Override
    @Transactional
    public void processRefundRequest(PaymentRefundRequest request) {
        log.info("Processing refund request for order: {}", request.getOrderId());
        
        paymentRepository.findByOrderId(request.getOrderId()).ifPresentOrElse(
                payment -> {
                    try {
                        if (payment.getPaymentIntentId() == null) {
                            log.error("Cannot refund order {}: No PaymentIntent found", request.getOrderId());
                            return;
                        }
                        
                        stripeService.refundPayment(payment.getPaymentIntentId());
                        
                        payment.setStatus("REFUNDED");
                        paymentRepository.save(payment);
                        log.info("Successfully refunded order {}", request.getOrderId());
                        
                    } catch (StripeException e) {
                        log.error("Failed to refund order {}: {}", request.getOrderId(), e.getMessage());
                    }
                },
                () -> log.error("Cannot refund order {}: Payment record not found", request.getOrderId())
        );
    }

    @Override
    @Transactional
    public void handlePaymentIntentSucceeded(String paymentIntentId, long amount) {
        paymentRepository.findByPaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus("SUCCEEDED");
            payment.setStripeStatus("succeeded");
            paymentRepository.save(payment);

            messagePublisher.publishPaymentSucceeded(PaymentSucceededEvent.builder()
                    .orderId(payment.getOrderId())
                    .paymentIntentId(paymentIntentId)
                    .amount(amount)
                    .build());
        });
    }

    @Override
    @Transactional
    public void handlePaymentIntentFailed(String paymentIntentId, String errorMessage) {
        paymentRepository.findByPaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus("FAILED");
            payment.setStripeStatus("requires_payment_method");
            payment.setErrorMessage(errorMessage);
            paymentRepository.save(payment);

            messagePublisher.publishPaymentFailed(PaymentFailedEvent.builder()
                    .orderId(payment.getOrderId())
                    .paymentIntentId(paymentIntentId)
                    .errorMessage(errorMessage)
                    .build());
        });
    }
}
