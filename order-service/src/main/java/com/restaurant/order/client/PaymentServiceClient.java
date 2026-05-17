package com.restaurant.order.client;

import com.restaurant.order.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class PaymentServiceClient {

    private final RestClient restClient;

    public PaymentServiceClient(@Value("${app.services.payment-url}") String paymentServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(paymentServiceUrl)
                .build();
    }

    /**
     * Makes a synchronous REST call to payment-service to create a Stripe PaymentIntent.
     * Returns the response containing the clientSecret and paymentIntentId.
     */
    public PaymentIntentResponse createPaymentIntent(Long orderId, Long clientId, BigDecimal amount, String currency) {
        log.info("Calling payment-service to create PaymentIntent for order: {}", orderId);

        Map<String, Object> requestBody = Map.of(
                "orderId", orderId,
                "clientId", clientId,
                "amount", amount,
                "currency", currency
        );

        try {
            PaymentIntentResponse response = restClient.post()
                    .uri("/api/payments/intent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(PaymentIntentResponse.class);

            if (response == null) {
                throw new PaymentException("Received null response from payment-service");
            }

            log.info("PaymentIntent created for order: {}, intentId: {}", orderId, response.paymentIntentId());
            return response;
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create PaymentIntent for order {}: {}", orderId, e.getMessage());
            throw new PaymentException("Failed to create payment intent: " + e.getMessage());
        }
    }

    public record PaymentIntentResponse(
            String clientSecret,
            String paymentIntentId,
            String status
    ) {}
}
