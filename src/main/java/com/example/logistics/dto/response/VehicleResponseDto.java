package com.example.logistics.dto.response;

import com.example.logistics.entity.enums.VehicleAvailability;
import com.example.logistics.entity.enums.VehicleType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class VehicleResponseDto {
    private UUID vehicleId;
    private String vehicleCode;
    private UUID transporterId;
    private String vehicleNumberPlate;
    private String engineNumber;
    private VehicleType vehicleType;
    private BigDecimal capacityKg;
    private String operationArea;
    private VehicleAvailability availabilityStatus;
    private Boolean isActive;
    private OffsetDateTime createdAt;
}