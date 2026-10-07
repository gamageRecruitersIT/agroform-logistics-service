package com.example.logistics.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.example.logistics.dto.response.VehiclePhotoResponseDto;
import com.example.logistics.entity.Vehicle;
import com.example.logistics.entity.VehiclePhoto;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.VehiclePhotoRepository;
import com.example.logistics.repository.VehicleRepository;
import com.example.logistics.service.VehiclePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehiclePhotoServiceImpl implements VehiclePhotoService {

    private final VehiclePhotoRepository photoRepository;
    private final VehicleRepository vehicleRepository;
    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String cloudinaryFolder;

    @Override
    @Transactional
    public VehiclePhotoResponseDto uploadPhoto(String vehicleCode, MultipartFile file, Boolean isPrimary, Integer displayOrder) {

        Vehicle vehicle = vehicleRepository.findByVehicleCode(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));

        try {

            Map<String, Object> uploadOptions = ObjectUtils.asMap(
                    "folder", cloudinaryFolder,
                    "transformation", new Transformation()
                            .width(1280)
                            .crop("limit")
                            .quality("auto:good")
                            .fetchFormat("auto")
            );

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);

            String photoUrl = (String) uploadResult.get("secure_url");

            if (isPrimary != null && isPrimary) {
                Optional<VehiclePhoto> existingPrimary = photoRepository.findByVehicleVehicleIdAndIsPrimaryTrue(vehicle.getVehicleId());
                existingPrimary.ifPresent(photo -> {
                    photo.setIsPrimary(false);
                    photoRepository.saveAndFlush(photo);
                });
            }

            VehiclePhoto newPhoto = VehiclePhoto.builder()
                    .vehicle(vehicle)
                    .photoUrl(photoUrl)
                    .isPrimary(isPrimary != null ? isPrimary : false)
                    .displayOrder(displayOrder != null ? displayOrder : 0)
                    .build();

            VehiclePhoto savedPhoto = photoRepository.save(newPhoto);
            return mapToResponseDto(savedPhoto);

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload photo to Cloudinary", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiclePhotoResponseDto> getVehiclePhotos(String vehicleCode) {

        Vehicle vehicle = vehicleRepository.findByVehicleCode(vehicleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with code: " + vehicleCode));

        return photoRepository.findByVehicleVehicleIdOrderByDisplayOrderAsc(vehicle.getVehicleId())
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