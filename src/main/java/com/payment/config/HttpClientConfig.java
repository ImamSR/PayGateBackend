package com.payment.config;


import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class HttpClientConfig {
    
    @Bean
    public RestTemplate restTemplate(
        final RestTemplateBuilder restTemplateBuilder,
        final MidtransProperties midtransProperties
    ){
        return restTemplateBuilder
        .connectTimeout(Duration.ofMillis(midtransProperties.getConnectionTimeoutMillis()))
        .readTimeout(Duration.ofMillis(midtransProperties.getReadTimeoutMillis()))
        .build();
    }
}
