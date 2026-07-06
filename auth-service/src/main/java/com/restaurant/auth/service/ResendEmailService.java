package com.restaurant.auth.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private final Resend resendClient;

    @Value("${app.email.from:noreply@revive.com}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String htmlContent = "<p>You requested a password reset. Click the link below to reset your password:</p>" +
                "<p><a href=\"" + resetLink + "\">Reset Password</a></p>" +
                "<p>This link will expire in 15 minutes.</p>";

        CreateEmailOptions sendEmailRequest = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(toEmail)
                .subject("Password Reset Request")
                .html(htmlContent)
                .build();

        try {
            resendClient.emails().send(sendEmailRequest);
            log.info("Password reset email sent to {}", toEmail);
        } catch (ResendException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
