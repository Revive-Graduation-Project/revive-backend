package com.restaurant.payment.service.impl;

import com.restaurant.payment.dto.PaymentFailedEvent;
import com.restaurant.payment.dto.PaymentIntentCreatedEvent;
import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentRefundRequest;
import com.restaurant.payment.dto.PaymentSucceededEvent;
import com.restaurant.payment.entity.Payment;
import com.restaurant.payment.messaging.MessagePublisher;
import com.restaurant.payment.repository.PaymentRepository;
import com.restaurant.payment.service.StripeService;
import com.stripe.exception.StripeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private StripeService stripeService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setOrderId(1L);
        paymentRequest.setAmount(new BigDecimal("10.50"));
        paymentRequest.setCurrency("usd");

        payment = Payment.builder()
                .id(1L)
                .orderId(1L)
                .amount(1050L)
                .currency("usd")
                .status("PENDING")
                .build();
    }

    @Test
    void processPaymentRequest_Success() throws StripeException {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        
        com.restaurant.payment.dto.PaymentResponse stripeResponse = new com.restaurant.payment.dto.PaymentResponse();
        stripeResponse.setPaymentIntentId("pi_123");
        stripeResponse.setClientSecret("secret_123");
        stripeResponse.setStatus("requires_payment_method");
        
        when(stripeService.createPaymentIntent(paymentRequest)).thenReturn(stripeResponse);

        paymentService.processPaymentRequest(paymentRequest);

        verify(paymentRepository, times(2)).save(any(Payment.class));
        assertThat(payment.getPaymentIntentId()).isEqualTo("pi_123");
        assertThat(payment.getStripeStatus()).isEqualTo("requires_payment_method");
    }

    @Test
    void processPaymentRequest_StripeException() throws StripeException {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));
        when(stripeService.createPaymentIntent(paymentRequest)).thenThrow(new com.stripe.exception.CardException(
            "Card declined", "req_123", "declined", "card_error", "declined", "charge_123", 402, null
        ));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
            paymentService.processPaymentRequest(paymentRequest);
        });

        verify(paymentRepository, times(2)).save(payment);
        assertThat(payment.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getErrorMessage()).contains("Card declined");
    }

    @Test
    void processRefundRequest_Success() throws StripeException {
        payment.setPaymentIntentId("pi_123");
        PaymentRefundRequest refundRequest = new PaymentRefundRequest();
        refundRequest.setOrderId(1L);
        
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        paymentService.processRefundRequest(refundRequest);

        verify(stripeService).refundPayment("pi_123");
        verify(paymentRepository).save(payment);
        assertThat(payment.getStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void handlePaymentIntentSucceeded() {
        payment.setPaymentIntentId("pi_123");
        when(paymentRepository.findByPaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

        paymentService.handlePaymentIntentSucceeded("pi_123", 1050L);

        verify(paymentRepository).save(payment);
        verify(messagePublisher).publishPaymentSucceeded(any(PaymentSucceededEvent.class));
        
        assertThat(payment.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(payment.getStripeStatus()).isEqualTo("succeeded");
    }

    @Test
    void handlePaymentIntentFailed() {
        payment.setPaymentIntentId("pi_123");
        when(paymentRepository.findByPaymentIntentId("pi_123")).thenReturn(Optional.of(payment));

        paymentService.handlePaymentIntentFailed("pi_123", "Insufficient funds");

        verify(paymentRepository).save(payment);
        verify(messagePublisher).publishPaymentFailed(any(PaymentFailedEvent.class));
        
        assertThat(payment.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getStripeStatus()).isEqualTo("requires_payment_method");
        assertThat(payment.getErrorMessage()).isEqualTo("Insufficient funds");
    }
}
