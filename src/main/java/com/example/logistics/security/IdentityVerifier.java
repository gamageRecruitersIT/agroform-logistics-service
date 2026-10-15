package com.example.logistics.security;

import com.example.logistics.exception.ForbiddenException;
import com.example.logistics.exception.UnauthorizedRoleException;
import com.example.logistics.feign.client.IdentityServiceClient;
import com.example.logistics.feign.dto.UserStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Eligibility check for users who are NOT the caller (e.g. the driver a transporter assigns):
 * their role/status can't come from the caller's JWT, so Identity & Access Service is asked.
 * The caller's own role is already enforced via @PreAuthorize on the JWT claim.
 * Feign failures propagate to GlobalExceptionHandler (404/502/503...).
 */
@Component
@RequiredArgsConstructor
public class IdentityVerifier {

    private final IdentityServiceClient identityServiceClient;

    public void requireActive(UUID userId, UserRole expectedRole) {
        UserStatusDto status = identityServiceClient.getUserStatus(userId).getData();
        if (status == null) {
            throw new ForbiddenException("Identity service returned no status for user " + userId);
        }
        if (!expectedRole.name().equalsIgnoreCase(status.roleCode())) {
            throw new UnauthorizedRoleException(
                    "User " + userId + " has role '" + status.roleCode() + "', expected " + expectedRole);
        }
        if (!"ACTIVE".equalsIgnoreCase(status.userStatus())) {
            throw new ForbiddenException("User " + userId + " is not ACTIVE (status: " + status.userStatus() + ")");
        }
    }
}
