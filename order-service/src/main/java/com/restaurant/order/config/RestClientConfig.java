package com.restaurant.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
public class RestClientConfig {

    private final JwtTokenInterceptor jwtTokenInterceptor;

    /**
     * Provides a RestClient.Builder bean that includes the JWT propagation interceptor.
     * Service clients should inject this builder to create their RestClient instances.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
                .requestInterceptor(jwtTokenInterceptor);
    }
}
