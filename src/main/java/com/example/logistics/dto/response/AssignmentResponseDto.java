package com.example.logistics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO returned after a successful driver + vehicle assignment.
 * Contains both the created TransportTask code and the DriverAssignment ID.
 */
@Getter
@Setter
@Builder
public class AssignmentResponseDto {

    private UUID assignmentId;

    private String transportTaskCode;
    private UUID transportRequestId;

    private UUID driverId;
    private UUID vehicleId;
    private UUID transporterId;

    private OffsetDateTime assignedAt;
    private Boolean isActive;
}
