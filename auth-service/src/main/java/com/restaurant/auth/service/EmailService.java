package com.restaurant.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
