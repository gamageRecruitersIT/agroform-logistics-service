package com.example.logistics.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRequestCreateDto {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    private String pickupAddress;

    private UUID warehouseId;

    private BigDecimal warehouseLatitude;

    private BigDecimal warehouseLongitude;

    private String notes;
}
