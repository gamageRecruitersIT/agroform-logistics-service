package com.example.logistics.service.impl;

import com.example.logistics.dto.request.VehicleRegistrationRequestDto;
import com.example.logistics.dto.response.VehicleResponseDto;
import com.example.logistics.entity.Vehicle;
import com.example.logistics.entity.enums.VehicleAvailability;
import com.example.logistics.exception.DuplicateResourceException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.exception.VehicleNotAvailableException;
import com.example.logistics.repository.VehicleRepository;
import com.example.logistics.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    @Override
    @Transactional
    public VehicleResponseDto registerVehicle(VehicleRegistrationRequestDto requestDto) {
        if (vehicleRepository.existsByVehicleNumberPlate(requestDto.getVehicleNumberPlate())) {
            throw new DuplicateResourceException("Vehicle with number plate " + requestDto.getVehicleNumberPlate() + " already exists.");
        }
        if (vehicleRepository.existsByEngineNumber(requestDto.getEngineNumber())) {
            throw new DuplicateResourceException("Vehicle with engine number " + requestDto.getEngineNumber() + " already exists.");
        }

        String generatedVehicleCode = "VHC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Vehicle vehicle = Vehicle.builder()
                .vehicleCode(generatedVehicleCode)
                .transporterId(requestDto.getTransporterId())
                .vehicleNumberPlate(requestDto.getVehicleNumberPlate())
                .engineNumber(requestDto.getEngineNumber())
                .vehicleType(requestDto.getVehicleType())
                .capacityKg(requestDto.getCapacityKg())
                .operationArea(requestDto.getOperationArea())
                .availabilityStatus(VehicleAvailability.AVAILABLE)
                .isActive(true)
                .build();

        return mapToResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponseDto> getVehiclesByTransporterId(UUID transporterId) {
        return vehicleRepository.findByTransporterId(transporterId).stream()
                .map(this::mapToResponseDto).collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public VehicleResponseDto getVehicleByCode(String vehicleCode) {
        return mapToResponseDto(fetchVehicleEntity(vehicleCode));
    }

    @Override
    @Transactional
    public VehicleResponseDto updateVehicleAvailability(String vehicleCode, VehicleAvailability newStatus) {
        if (newStatus == VehicleAvailability.ASSIGNED) {
            throw new IllegalArgumentException("Cannot manually set status to ASSIGNED");
        }

        Vehicle vehicle = vehicleRepository.findByVehicleCodeForUpdate(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));

        if (vehicle.getAvailabilityStatus() == VehicleAvailability.ASSIGNED) {
            throw new VehicleNotAvailableException("Cannot change status of a currently ASSIGNED vehicle");
        }

        vehicle.setAvailabilityStatus(newStatus);
        vehicle.setIsActive(newStatus != VehicleAvailability.INACTIVE);

        return mapToResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    public void markVehicleAssigned(String vehicleCode) {
        Vehicle vehicle = vehicleRepository.findByVehicleCodeForUpdate(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));

        ensureAssignable(vehicle);

        vehicle.setAvailabilityStatus(VehicleAvailability.ASSIGNED);
        vehicleRepository.save(vehicle);
    }

    @Override
    @Transactional
    public void releaseVehicleAfterTask(String vehicleCode) {
        Vehicle vehicle = vehicleRepository.findByVehicleCodeForUpdate(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));

        if (vehicle.getAvailabilityStatus() == VehicleAvailability.ASSIGNED) {
            vehicle.setAvailabilityStatus(VehicleAvailability.AVAILABLE);
            vehicleRepository.save(vehicle);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateVehicleAvailabilityForAssignment(String vehicleCode) {
        ensureAssignable(fetchVehicleEntity(vehicleCode));
    }


    private void ensureAssignable(Vehicle vehicle) {
        if (!vehicle.getIsActive() || vehicle.getAvailabilityStatus() != VehicleAvailability.AVAILABLE) {
            throw new VehicleNotAvailableException(
                    "Vehicle " + vehicle.getVehicleNumberPlate() + " is currently " + vehicle.getAvailabilityStatus() + " and cannot be assigned."
            );
        }
    }

    private Vehicle fetchVehicleEntity(String vehicleCode) {
        return vehicleRepository.findByVehicleCode(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));
    }

    private VehicleResponseDto mapToResponseDto(Vehicle vehicle) {
        VehicleResponseDto dto = new VehicleResponseDto();
        dto.setVehicleId(vehicle.getVehicleId());
        dto.setVehicleCode(vehicle.getVehicleCode());
        dto.setTransporterId(vehicle.getTransporterId());
        dto.setVehicleNumberPlate(vehicle.getVehicleNumberPlate());
        dto.setEngineNumber(vehicle.getEngineNumber());
        dto.setVehicleType(vehicle.getVehicleType());
        dto.setCapacityKg(vehicle.getCapacityKg());
        dto.setOperationArea(vehicle.getOperationArea());
        dto.setAvailabilityStatus(vehicle.getAvailabilityStatus());
        dto.setIsActive(vehicle.getIsActive());
        dto.setCreatedAt(vehicle.getCreatedAt());
        return dto;
    }
}