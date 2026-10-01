package com.example.logistics.dto.response;

import com.example.logistics.entity.enums.TransportTaskStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for TransportTask.
 * Exposes task_code (public) instead of internal UUIDs where applicable.
 */
@Getter
@Setter
@Builder
public class TaskResponseDto {

    private UUID transportTaskId;
    private String transportTaskCode;

    private UUID transportRequestId;
    private UUID transporterId;
    private UUID vehicleId;
    private UUID driverId;

    private OffsetDateTime assignedAt;
    private OffsetDateTime acceptedAt;

    private BigDecimal estimatedCost;
    private BigDecimal actualCost;

    private TransportTaskStatus taskStatus;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
