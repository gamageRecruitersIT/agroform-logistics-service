package com.example.logistics.service;

import com.example.logistics.dto.VehicleRegistrationRequestDto;
import com.example.logistics.dto.response.VehicleResponseDto;
import com.example.logistics.entity.enums.VehicleAvailability;

import java.util.List;
import java.util.UUID;

public interface VehicleService {
    VehicleResponseDto registerVehicle(VehicleRegistrationRequestDto requestDto);

    VehicleResponseDto getVehicleById(UUID vehicleId);

    List<VehicleResponseDto> getVehiclesByTransporterId(UUID transporterId);

    VehicleResponseDto updateVehicleAvailability(UUID vehicleId, VehicleAvailability newStatus);

    void validateVehicleAvailabilityForAssignment(UUID vehicleId);
}