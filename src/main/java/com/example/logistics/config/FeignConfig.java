package com.example.logistics.config;

import feign.Logger;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Shared Feign configuration for all outbound clients.
// Forwards the caller's Authorization header (JWT) onto downstream Feign calls.
@Configuration
public class FeignConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Intercepts outgoing Feign requests and attaches the Authorization token
     * coming from the incoming HTTP request.
     */
    @Bean
    public RequestInterceptor authorizationHeaderInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                requestTemplate.header(AUTHORIZATION_HEADER, authorizationHeader);
            }
        };
    }

    /**
     * Enables basic Feign request/response logging.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}