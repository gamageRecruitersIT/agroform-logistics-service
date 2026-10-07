package com.example.logistics.event;

import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.repository.TransportTaskRef;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Published when a delay flag is raised. Navodya listens to send the delay notification;
 * Gayani listens to log/publish a delay event.
 */
public record DeliveryDelayedEvent(
        TransportTaskRef task,
        DeliveryStatusEnum currentStatus,
        String reason,
        UUID flaggedBy,
        OffsetDateTime occurredAt) {
}
