package com.example.logistics.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class FeignConfig {

    /**
     * Intercepts outgoing Feign requests and attaches the Authorization token
     * coming from the incoming HTTP request.
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // Get the Authorization header from the incoming request (Postman -> Logistics)
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null) {
                    // Attach it to the outgoing request (Logistics -> Order Service)
                    requestTemplate.header("Authorization", authHeader);
                }
            }
        };
    }
}
