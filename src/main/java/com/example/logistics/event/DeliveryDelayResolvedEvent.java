package com.example.logistics.event;

import com.example.logistics.entity.enums.DeliveryStatusEnum;
import com.example.logistics.repository.TransportTaskRef;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Published when a delay flag is cleared. */
public record DeliveryDelayResolvedEvent(
        TransportTaskRef task,
        DeliveryStatusEnum currentStatus,
        UUID resolvedBy,
        String notes,
        OffsetDateTime occurredAt) {
}
