package com.example.orderservice.integration.testauth;

import com.example.orderservice.infrastructure.security.JwtService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestAuthConfig {
    @Bean
    public TestAuthController testAuthController(JwtService jwtService) {
        return new TestAuthController(jwtService);
    }
}
