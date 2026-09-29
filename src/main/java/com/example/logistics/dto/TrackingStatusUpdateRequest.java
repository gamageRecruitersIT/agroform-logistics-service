package com.example.logistics.dto;

import com.example.logistics.entity.DeliveryStatusEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Submitted by the assigned DRIVER to move a task's delivery status exactly one step
 * forward. The TRANSPORTER may only use this endpoint with authorizedOverride = true
 * (to correct a backward move or a skipped step). The acting user comes from the
 * authenticated caller - it is NOT part of the request body.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TrackingStatusUpdateRequest {

    @NotNull(message = "newStatus is required")
    private DeliveryStatusEnum newStatus;

    @DecimalMin(value = "-90.0", message = "latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "latitude must be <= 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "longitude must be <= 180")
    private BigDecimal longitude;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    private String notes;

    /** Only honoured for the TRANSPORTER role. Allows a backward move or a skipped step. */
    private boolean authorizedOverride = false;
}
