package com.restaurant.auth.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfig {

    @Value("${resend.api.key:re_dummy_key_for_tests}")
    private String resendApiKey;

    @Bean
    public Resend resend() {
        return new Resend(resendApiKey);
    }
}
