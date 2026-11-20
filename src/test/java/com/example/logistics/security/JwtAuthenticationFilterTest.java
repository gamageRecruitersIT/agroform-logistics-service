package com.example.logistics.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesUserIdClaimWhenJwtSubjectIsEmail() throws Exception {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        Claims claims = mock(Claims.class);
        UUID userId = UUID.randomUUID();
        when(jwtTokenProvider.parseClaims("token")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.getSubject()).thenReturn("transporter@example.com");
        when(claims.get("role", String.class)).thenReturn("TRANSPORTER");

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {};

        filter.doFilter(request, response, chain);

        assertEquals(userId.toString(), SecurityContextHolder.getContext().getAuthentication().getName());
        assertEquals("ROLE_TRANSPORTER", SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority());
    }
}
