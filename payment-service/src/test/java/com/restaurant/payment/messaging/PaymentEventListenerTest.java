package com.restaurant.payment.messaging;

import com.restaurant.payment.dto.PaymentRequest;
import com.restaurant.payment.dto.PaymentRefundRequest;
import com.restaurant.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentEventListener paymentEventListener;



    @Test
    void handleRefundRequest_ShouldCallProcessRefundRequest() {
        PaymentRefundRequest request = new PaymentRefundRequest();
        request.setOrderId(1L);

        paymentEventListener.handleRefundRequest(request);

        verify(paymentService).processRefundRequest(request);
    }
}
