package com.example.logistics.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record LogisticsKafkaEvent(
        String eventId,
        String eventType,
        String transportRequestCode,
        String taskCode,
        String vehicleCode,
        String description,
        String createdBy,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime createdAt
) {
}