package com.restaurant.menu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SupabaseStorageConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String supabaseServiceRoleKey;

    @Bean("supabaseRestClient")
    public RestClient supabaseRestClient() {
        return RestClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1")
                .defaultHeader("Authorization", "Bearer " + supabaseServiceRoleKey)
                .defaultHeader("apikey", supabaseServiceRoleKey)
                .build();
    }
}
