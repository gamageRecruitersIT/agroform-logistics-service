package com.example.logistics.service;

import com.example.logistics.dto.response.VehiclePhotoResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface VehiclePhotoService {
    VehiclePhotoResponseDto uploadPhoto(String vehicleCode, MultipartFile file, Boolean isPrimary, Integer displayOrder);

    List<VehiclePhotoResponseDto> getVehiclePhotos(String vehicleCode);
}