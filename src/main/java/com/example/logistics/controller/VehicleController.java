package com.example.logistics.controller;

import com.example.logistics.dto.request.VehicleRegistrationRequestDto;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.VehicleResponseDto;
import com.example.logistics.entity.enums.VehicleAvailability;
import com.example.logistics.service.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistics/vehicles")
@RequiredArgsConstructor
@Validated
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponseDto>> registerVehicle(
            @Valid @RequestBody VehicleRegistrationRequestDto requestDto) {

        VehicleResponseDto response = vehicleService.registerVehicle(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vehicle registered successfully", response));
    }

    @GetMapping("/{vehicleCode}")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> getVehicleByCode(
            @PathVariable String vehicleCode) {
        VehicleResponseDto response = vehicleService.getVehicleByCode(vehicleCode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle fetched successfully", response));
    }


    @GetMapping("/transporter/{transporterId}")
    public ResponseEntity<ApiResponse<List<VehicleResponseDto>>> getVehiclesByTransporterId(
            @PathVariable UUID transporterId) {

        List<VehicleResponseDto> response = vehicleService.getVehiclesByTransporterId(transporterId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Transporter vehicles fetched successfully", response)
        );
    }


    @PatchMapping("/{vehicleCode}/status")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> updateVehicleAvailability(
            @PathVariable String vehicleCode,
            @RequestParam String status) {

        VehicleAvailability parsedStatus = VehicleAvailability.fromApi(status);
        VehicleResponseDto response = vehicleService.updateVehicleAvailability(vehicleCode, parsedStatus);
        return ResponseEntity.ok(new ApiResponse<>(true, "Vehicle availability updated successfully", response));
    }
}