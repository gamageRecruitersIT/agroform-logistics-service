package com.example.logistics.dto.request;

import com.example.logistics.entity.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class VehicleRegistrationRequestDto {

    @NotNull(message = "Transporter ID is required")
    private UUID transporterId;

    @NotBlank(message = "Vehicle number plate is required")
    private String vehicleNumberPlate;

    @NotBlank(message = "Engine number is required")
    private String engineNumber;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicleType;

    @NotNull(message = "Capacity is required")
    @DecimalMin(value = "0.1", message = "Capacity must be strictly positive")
    private BigDecimal capacityKg;

    @NotBlank(message = "Operation area is required")
    private String operationArea;
}