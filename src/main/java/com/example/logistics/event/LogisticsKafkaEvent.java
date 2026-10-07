package com.example.logistics.dto.event;

import java.time.OffsetDateTime;

public record LogisticsKafkaEvent(
        String eventId,
        String eventType,
        String transportRequestCode,
        String taskCode,
        String vehicleCode,
        String description,
        String createdBy,
        OffsetDateTime createdAt
) {
}