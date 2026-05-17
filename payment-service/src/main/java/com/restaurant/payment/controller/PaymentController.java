package com.restaurant.payment.controller;

import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentResponse;
import com.restaurant.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent")
    public ResponseEntity<PaymentResponse> createPaymentIntent(@RequestBody PaymentRequest request) {
        log.info("Received synchronous request to create payment intent for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPaymentRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
