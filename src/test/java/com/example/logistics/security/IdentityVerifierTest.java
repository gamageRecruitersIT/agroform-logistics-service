package com.example.logistics.security;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.exception.UnauthorizedRoleException;
import com.example.logistics.feign.client.IdentityServiceClient;
import com.example.logistics.feign.dto.UserStatusDto;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityVerifierTest {

    @Test
    void forwardsAuthorizationHeaderWhenCheckingDriverEligibility() {
        IdentityServiceClient identityServiceClient = mock(IdentityServiceClient.class);
        IdentityVerifier identityVerifier = new IdentityVerifier(identityServiceClient);
        UUID driverId = UUID.randomUUID();
        String authorizationHeader = "Bearer incoming-jwt";

        when(identityServiceClient.getUserStatus(driverId, authorizationHeader)).thenReturn(
                ApiResponse.success("User status fetched", new UserStatusDto(
                        driverId, "DRV-123", "ACTIVE", "ROLE-002"))
        );

        assertDoesNotThrow(() ->
                identityVerifier.requireActive(driverId, UserRole.DRIVER, authorizationHeader));

        verify(identityServiceClient).getUserStatus(driverId, authorizationHeader);
    }

    @Test
    void rejectsIdentityRoleCodeForAnotherRole() {
        IdentityServiceClient identityServiceClient = mock(IdentityServiceClient.class);
        IdentityVerifier identityVerifier = new IdentityVerifier(identityServiceClient);
        UUID userId = UUID.randomUUID();

        when(identityServiceClient.getUserStatus(userId, "Bearer incoming-jwt")).thenReturn(
                ApiResponse.success("User status fetched", new UserStatusDto(
                        userId, "TRN-123", "ACTIVE", "ROLE-001"))
        );

        assertThrows(UnauthorizedRoleException.class,
                () -> identityVerifier.requireActive(userId, UserRole.DRIVER, "Bearer incoming-jwt"));
    }
}
