package com.example.logistics.dto.response;

import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class VehiclePhotoResponseDto {
    private UUID photoId;
    private UUID vehicleId;
    private String photoUrl;
    private Boolean isPrimary;
    private Integer displayOrder;
    private OffsetDateTime uploadedAt;
}