package com.example.logistics.dto.request;

import com.example.logistics.entity.DeliveryStatusEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Submitted by the driver (or transporter, for an authorized override) to move a
 * transport task's delivery status forward. Normal submissions must be exactly one
 * step forward in the lifecycle; authorizedOverride is required for any backward
 * or skipped transition and is expected to be gated behind a TRANSPORTER-role check
 * upstream (Navodya's authorization module).
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

    @NotNull(message = "updatedBy is required")
    private UUID updatedBy;

    private String notes;

    /**
     * Must be true to allow a backward move or a skipped step.
     * Defaults to false so normal driver submissions are always strictly sequential.
     */
    private boolean authorizedOverride = false;
}
