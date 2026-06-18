package com.restaurant.payment.messaging;

import com.restaurant.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PaymentService paymentService;

    @RabbitListener(queues = "${app.rabbitmq.queues.payment-refund.name}")
    public void handleRefundRequest(com.restaurant.payment.dto.PaymentRefundRequest request) {
        log.info("Received refund request for order: {}", request.getOrderId());
        paymentService.processRefundRequest(request);
    }
}
