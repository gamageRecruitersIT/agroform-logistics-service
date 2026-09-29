package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.util.UUID;

@Data
public class CostPreviewRequest {


    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;

    private UUID transportRequestId;

    @NotNull(message = "Product weight is required")
    @Positive(message = "Product weight must be greater than zero")
    private Double productWeight;


    @NotNull(message = "Origin latitude (Farm) is required")
    private Double originLat;

    @NotNull(message = "Origin longitude (Farm) is required")
    private Double originLng;


    @NotNull(message = "Destination latitude (Warehouse) is required")
    private Double destinationLat;

    @NotNull(message = "Destination longitude (Warehouse) is required")
    private Double destinationLng;
}