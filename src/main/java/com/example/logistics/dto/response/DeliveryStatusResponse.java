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

    /** Public code (e.g. TTK-AB12CD34). Internal UUIDs are never exposed. */
    private String transportTaskCode;
    private DeliveryStatusEnum currentStatus;
    private boolean delayed;
    private String delayReason;
    private OffsetDateTime delayedAt;
    private UUID updatedBy;
    private String notes;
    private OffsetDateTime updatedAt;
}
