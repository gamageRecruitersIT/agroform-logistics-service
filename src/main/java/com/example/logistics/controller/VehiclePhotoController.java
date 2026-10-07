package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.VehiclePhotoResponseDto;
import com.example.logistics.service.VehiclePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logistics/vehicles")
@RequiredArgsConstructor
public class VehiclePhotoController {

    private final VehiclePhotoService vehiclePhotoService;

    @PostMapping(value = "/{vehicleCode}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehiclePhotoResponseDto>> uploadPhoto(
            @PathVariable String vehicleCode,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder) {

        VehiclePhotoResponseDto response = vehiclePhotoService.uploadPhoto(vehicleCode, file, isPrimary, displayOrder);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vehicle photo uploaded successfully", response));
    }

    @GetMapping("/{vehicleCode}/photos")
    public ResponseEntity<ApiResponse<List<VehiclePhotoResponseDto>>> getVehiclePhotos(
            @PathVariable String vehicleCode) {

        List<VehiclePhotoResponseDto> response = vehiclePhotoService.getVehiclePhotos(vehicleCode);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Vehicle photos fetched successfully", response)
        );
    }
}