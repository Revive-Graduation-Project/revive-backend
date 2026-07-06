package com.restaurant.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.Emails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResendEmailServiceTest {

    @Mock
    private Resend resendClient;

    @Mock
    private Emails emailsMock;

    private ResendEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new ResendEmailService(resendClient);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@revive.com");
    }

    @Test
    void testSendPasswordResetEmail_Success() throws ResendException {
        // Arrange
        String toEmail = "test@example.com";
        String resetLink = "http://localhost:3000/reset-password?token=12345";
        
        when(resendClient.emails()).thenReturn(emailsMock);
        when(emailsMock.send(any(CreateEmailOptions.class))).thenReturn(new CreateEmailResponse());

        // Act
        emailService.sendPasswordResetEmail(toEmail, resetLink);

        // Assert
        ArgumentCaptor<CreateEmailOptions> requestCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emailsMock, times(1)).send(requestCaptor.capture());
        
        CreateEmailOptions sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.getTo()).contains(toEmail);
        assertThat(sentRequest.getFrom()).isEqualTo("noreply@revive.com");
        assertThat(sentRequest.getSubject()).isEqualTo("Password Reset Request");
        assertThat(sentRequest.getHtml()).contains(resetLink);
    }
}
