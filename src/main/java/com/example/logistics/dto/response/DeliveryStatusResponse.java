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
    /** Public code of the parent transport request (e.g. TRQ-AB12CD34), so the farmer can link request -> tracking. */
    private String transportRequestCode;
    /** Public vehicle code (e.g. VHC-AB12CD34) of the vehicle doing this delivery. */
    private String vehicleCode;
    private DeliveryStatusEnum currentStatus;
    private boolean delayed;
    private String delayReason;
    private OffsetDateTime delayedAt;
    private UUID updatedBy;
    private String notes;
    private OffsetDateTime updatedAt;
}
