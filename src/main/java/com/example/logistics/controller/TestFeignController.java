package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.feign.client.CommunicationClient;
import com.example.logistics.feign.client.IdentityServiceClient;
import com.example.logistics.feign.dto.DelayNotificationRequest;
import com.example.logistics.feign.dto.DeliveryStatusNotificationRequest;
import com.example.logistics.feign.dto.NotificationResponseDTO;
import com.example.logistics.feign.dto.TransportAssignmentNotificationRequest;
import com.example.logistics.feign.dto.UserStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

// Manual verification that the two Feign clients reach their target services.
// Requires identity-service / communication-service to actually be running at
// the configured URLs — a connection-refused response here is a valid,
// informative test result, not a bug in this controller.
@RestController
@RequestMapping("/api/test/feign")
@RequiredArgsConstructor
public class TestFeignController {

    private final CommunicationClient communicationClient;
    private final IdentityServiceClient identityServiceClient;

    @GetMapping("/identity/status/{userId}")
    public ApiResponse<UserStatusDto> testUserStatus(@PathVariable UUID userId) {
        return identityServiceClient.getUserStatus(userId);
    }

    @PostMapping("/communication/transport-assignment")
    public ApiResponse<NotificationResponseDTO> testTransportAssignmentNotification() {
        TransportAssignmentNotificationRequest request = new TransportAssignmentNotificationRequest(
                "TR-TEST-0001",
                "TASK-TEST-0001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "VEH-TEST-0001",
                Instant.now()
        );

        return communicationClient.sendTransportAssignmentNotification(request);
    }

    @PostMapping("/communication/delay")
    public ApiResponse<NotificationResponseDTO> testDelayNotification() {
        DelayNotificationRequest request = new DelayNotificationRequest(
                "TR-TEST-0001",
                "TASK-TEST-0001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Heavy traffic on route",
                30,
                Instant.now()
        );

        return communicationClient.sendDelayNotification(request);
    }

    @PostMapping("/communication/delivery-status")
    public ApiResponse<NotificationResponseDTO> testDeliveryStatusNotification() {
        DeliveryStatusNotificationRequest request = new DeliveryStatusNotificationRequest(
                "TR-TEST-0001",
                "TASK-TEST-0001",
                UUID.randomUUID(),
                UUID.randomUUID(),
                "IN_TRANSIT",
                Instant.now()
        );

        return communicationClient.sendDeliveryStatusNotification(request);
    }
}
