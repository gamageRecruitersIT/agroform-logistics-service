package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Request payload for creating a driver + vehicle assignment to a transport request.
 * Submitted by a TRANSPORTER via POST /api/v1/logistics/assignments.
 */
@Getter
@Setter
@NoArgsConstructor
public class AssignmentRequestDto {

    @NotNull(message = "Transport request code is required")
    private String transportRequestCode;

    @NotNull(message = "Vehicle ID is required")
    private UUID vehicleId;

    @NotNull(message = "Driver ID is required")
    private UUID driverId;
}
