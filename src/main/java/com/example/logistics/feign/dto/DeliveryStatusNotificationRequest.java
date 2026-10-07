package com.example.logistics.feign.dto;

import java.time.Instant;
import java.util.UUID;

// Outbound payload to Communication & Support Service on each delivery status transition
// (AWAITING_PICKUP -> LOADED -> IN_TRANSIT -> DELIVERED -> UNLOADED_AT_WAREHOUSE).
public record DeliveryStatusNotificationRequest(
        String transportRequestCode,
        String taskCode,
        UUID driverId,
        UUID farmerId,
        String status,
        Instant statusUpdatedAt
) {}
