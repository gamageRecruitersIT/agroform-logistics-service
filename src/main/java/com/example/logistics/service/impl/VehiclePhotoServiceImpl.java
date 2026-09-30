package com.example.logistics.service.impl;

import com.example.logistics.dto.response.VehiclePhotoResponseDto;
import com.example.logistics.entity.Vehicle;
import com.example.logistics.entity.VehiclePhoto;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.VehiclePhotoRepository;
import com.example.logistics.repository.VehicleRepository;
import com.example.logistics.service.VehiclePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehiclePhotoServiceImpl implements VehiclePhotoService {

    private final VehiclePhotoRepository photoRepository;
    private final VehicleRepository vehicleRepository;


    private static final String UPLOAD_DIRECTORY = "uploads/vehicle-photos/";

    @Override
    @Transactional
    public VehiclePhotoResponseDto uploadPhoto(UUID vehicleId, MultipartFile file, Boolean isPrimary, Integer displayOrder) {


        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));

        try {

            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName != null ? originalFileName.substring(originalFileName.lastIndexOf(".")) : ".jpg";
            String newFileName = UUID.randomUUID().toString() + fileExtension;

            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            if (isPrimary != null && isPrimary) {
                Optional<VehiclePhoto> existingPrimary = photoRepository.findByVehicleVehicleIdAndIsPrimaryTrue(vehicleId);
                existingPrimary.ifPresent(photo -> {
                    photo.setIsPrimary(false);
                    photoRepository.save(photo);
                });
            }


            String photoUrl = "/api/v1/logistics/vehicles/photos/images/" + newFileName;

            VehiclePhoto newPhoto = VehiclePhoto.builder()
                    .vehicle(vehicle)
                    .photoUrl(photoUrl)
                    .isPrimary(isPrimary != null ? isPrimary : false)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .build();

            VehiclePhoto savedPhoto = photoRepository.save(newPhoto);
            return mapToResponseDto(savedPhoto);

        } catch (IOException e) {
            throw new RuntimeException("Failed to save photo locally", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiclePhotoResponseDto> getVehiclePhotos(UUID vehicleId) {

        if (!vehicleRepository.existsById(vehicleId)) {
            throw new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId);
        }

        return photoRepository.findByVehicleVehicleIdOrderByDisplayOrderAsc(vehicleId)
                .stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    private VehiclePhotoResponseDto mapToResponseDto(VehiclePhoto photo) {
        VehiclePhotoResponseDto dto = new VehiclePhotoResponseDto();
        dto.setPhotoId(photo.getPhotoId());
        dto.setVehicleId(photo.getVehicle().getVehicleId());
        dto.setPhotoUrl(photo.getPhotoUrl());
        dto.setIsPrimary(photo.getIsPrimary());
        dto.setDisplayOrder(photo.getDisplayOrder());
        dto.setUploadedAt(photo.getUploadedAt());
        return dto;
    }
}