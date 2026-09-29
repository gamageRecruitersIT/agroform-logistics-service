package com.example.logistics.dto.response;

import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.entity.TrackingUpdateType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingUpdateResponse {

    private String transportTaskCode;
    private TrackingUpdateType updateType;
    private DeliveryStatusEnum previousStatus;
    private DeliveryStatusEnum newStatus;
    private boolean delayed;
    private String delayReason;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private UUID updatedBy;
    private String notes;
    private OffsetDateTime recordedAt;
}
