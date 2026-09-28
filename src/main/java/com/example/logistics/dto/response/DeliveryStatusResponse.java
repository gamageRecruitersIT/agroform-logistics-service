package com.example.logistics.dto.response;

import com.example.logistics.entity.DeliveryStatusEnum;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatusResponse {

    private UUID transportTaskId;
    private DeliveryStatusEnum currentStatus;
    private boolean delayed;
    private String delayReason;
    private OffsetDateTime delayedAt;
    private UUID updatedBy;
    private String notes;
    private OffsetDateTime updatedAt;
}
