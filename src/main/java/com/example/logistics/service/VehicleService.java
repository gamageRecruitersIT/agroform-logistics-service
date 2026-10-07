package com.example.logistics.service;

import com.example.logistics.dto.request.VehicleRegistrationRequestDto;
import com.example.logistics.dto.response.VehicleResponseDto;
import com.example.logistics.entity.enums.VehicleAvailability;

import java.util.List;
import java.util.UUID;

public interface VehicleService {

    VehicleResponseDto registerVehicle(VehicleRegistrationRequestDto requestDto);

    List<VehicleResponseDto> getVehiclesByTransporterId(UUID transporterId);

    VehicleResponseDto getVehicleByCode(String vehicleCode);

    VehicleResponseDto updateVehicleAvailability(String vehicleCode, VehicleAvailability newStatus);

    void validateVehicleAvailabilityForAssignment(String vehicleCode);

    void markVehicleAssigned(String vehicleCode);

    void releaseVehicleAfterTask(String vehicleCode);
}