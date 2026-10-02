package com.example.logistics.feign.client;

import com.example.logistics.config.FeignConfig;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.feign.dto.DelayNotificationRequest;
import com.example.logistics.feign.dto.DeliveryStatusNotificationRequest;
import com.example.logistics.feign.dto.NotificationResponseDTO;
import com.example.logistics.feign.dto.TransportAssignmentNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// Notifies farmer/transporter/driver via Communication & Support Service for
// transport assignment, delay, and delivery status events.
@FeignClient(
        name = "communication-service",
        url = "${communication.service.url}",
        configuration = FeignConfig.class
)
public interface CommunicationClient {

    // Sent once a driver/vehicle is confirmed for a transport task.
    @PostMapping("/api/notifications/transport-assignment")
    ApiResponse<NotificationResponseDTO> sendTransportAssignmentNotification(
            @RequestBody TransportAssignmentNotificationRequest request
    );

    // Sent when a transport task is flagged DELAYED.
    @PostMapping("/api/notifications/transport-delay")
    ApiResponse<NotificationResponseDTO> sendDelayNotification(
            @RequestBody DelayNotificationRequest request
    );

    // Sent on each delivery status transition.
    @PostMapping("/api/notifications/delivery-status")
    ApiResponse<NotificationResponseDTO> sendDeliveryStatusNotification(
            @RequestBody DeliveryStatusNotificationRequest request
    );
}
