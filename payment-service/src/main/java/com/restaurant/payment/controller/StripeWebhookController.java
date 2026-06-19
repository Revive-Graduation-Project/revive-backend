package com.restaurant.payment.controller;

import com.restaurant.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @Value("${stripe.webhook-secret}")
    private String endpointSecret;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        }

        log.info("Received Stripe event: {}", event.getType());

        var deserializer = event.getDataObjectDeserializer();
        if (!deserializer.getObject().isPresent()) {
            log.warn("Could not deserialize Stripe event data object for event type: {}", event.getType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid event payload");
        }

        var stripeObject = deserializer.getObject().get();

        if ("payment_intent.succeeded".equals(event.getType()) && stripeObject instanceof PaymentIntent intent) {
            log.info("Payment Succeeded for intent: {}", intent.getId());
            paymentService.handlePaymentIntentSucceeded(intent.getId(), intent.getAmount());
        } else if ("payment_intent.payment_failed".equals(event.getType()) && stripeObject instanceof PaymentIntent intent) {
            log.info("Payment Failed for intent: {}", intent.getId());
            String error = intent.getLastPaymentError() != null ? intent.getLastPaymentError().getMessage() : "Unknown error";
            paymentService.handlePaymentIntentFailed(intent.getId(), error);
        }

        return ResponseEntity.ok("Success");
    }
}
