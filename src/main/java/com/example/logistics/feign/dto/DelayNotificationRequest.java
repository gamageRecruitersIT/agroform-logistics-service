package com.example.logistics.feign.dto;

import java.time.Instant;
import java.util.UUID;

// Outbound payload to Communication & Support Service when a transport task is flagged DELAYED.
public record DelayNotificationRequest(
        String transportRequestCode,
        String taskCode,
        UUID driverId,
        UUID farmerId,
        String delayReason,
        Integer estimatedDelayMinutes,
        Instant delayedAt
) {}
