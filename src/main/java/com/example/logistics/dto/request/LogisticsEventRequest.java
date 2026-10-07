package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LogisticsEventRequest(
        @NotBlank(message = "transportRequestCode is required")
        @Size(max = 20, message = "transportRequestCode must not exceed 20 characters")
        String transportRequestCode,
        @Size(max = 20, message = "taskCode must not exceed 20 characters")
        String taskCode,
        @Size(max = 20, message = "vehicleCode must not exceed 20 characters")
        String vehicleCode,
        @NotBlank(message = "eventType is required")
        @Size(max = 40, message = "eventType must not exceed 40 characters")
        String eventType,
        @Size(max = 500, message = "description must not exceed 500 characters")
        String description,
        @Pattern(
                regexp = "^$|^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
                message = "createdBy must be a valid UUID"
        )
        String createdBy
) {
}