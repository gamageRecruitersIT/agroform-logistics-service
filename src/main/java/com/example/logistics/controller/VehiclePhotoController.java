package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.VehiclePhotoResponseDto;
import com.example.logistics.service.VehiclePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistics/vehicles")
@RequiredArgsConstructor
public class VehiclePhotoController {

    private final VehiclePhotoService vehiclePhotoService;
    private static final String UPLOAD_DIRECTORY = "uploads/vehicle-photos/";

    @PostMapping(value = "/{vehicleId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VehiclePhotoResponseDto>> uploadPhoto(
            @PathVariable UUID vehicleId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary,
            @RequestParam(value = "displayOrder", defaultValue = "0") Integer displayOrder) {

        VehiclePhotoResponseDto response = vehiclePhotoService.uploadPhoto(vehicleId, file, isPrimary, displayOrder);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Vehicle photo uploaded successfully", response));
    }

    @GetMapping("/{vehicleId}/photos")
    public ResponseEntity<ApiResponse<List<VehiclePhotoResponseDto>>> getVehiclePhotos(
            @PathVariable UUID vehicleId) {

        List<VehiclePhotoResponseDto> response = vehiclePhotoService.getVehiclePhotos(vehicleId);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Vehicle photos fetched successfully", response)
        );
    }


    @GetMapping("/photos/images/{fileName}")
    public ResponseEntity<Resource> serveImage(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIRECTORY).resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()

                        .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}