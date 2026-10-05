package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// DEV-ONLY: mints a JWT with a "role" claim, signed with this service's own
// jwt.secret, so @PreAuthorize can be tested locally without a real Identity
// Service login. Remove once real login tokens carry "role" for every user.
@RestController
@RequestMapping("/api/test")
public class TestTokenController {

    private final SecretKey signingKey;

    public TestTokenController(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // role: TRANSPORTER | DRIVER | FARMER
    @GetMapping("/token")
    public ApiResponse<String> mintTestToken(
            @RequestParam String role,
            @RequestParam(required = false) String userId
    ) {
        String subject = userId != null ? userId : UUID.randomUUID().toString();

        String token = Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(signingKey)
                .compact();

        return ApiResponse.success("Test token minted for role " + role, token);
    }
}
