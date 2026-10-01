package com.example.logistics.feign.client;

import com.example.logistics.config.FeignConfig;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.feign.dto.UserStatusDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

// Verifies transporter/driver eligibility (active status, correct role) before assignment.
// Use only as a fallback when JWT claims don't already carry the needed status/role.
@FeignClient(
        name = "identity-service",
        url = "${identity.service.url}",
        configuration = FeignConfig.class
)
public interface IdentityServiceClient {

    @GetMapping("/api/users/{userId}/status")
    ApiResponse<UserStatusDto> getUserStatus(@PathVariable("userId") UUID userId);
}
