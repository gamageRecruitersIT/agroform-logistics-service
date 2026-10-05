package com.example.logistics.feign.dto;

import java.time.Instant;
import java.util.UUID;

// Outbound payload to Communication & Support Service once a driver/vehicle is
// confirmed for a transport task.
public record TransportAssignmentNotificationRequest(
        String transportRequestCode,
        String taskCode,
        UUID driverId,
        UUID farmerId,
        String vehicleCode,
        Instant assignedAt
) {}
