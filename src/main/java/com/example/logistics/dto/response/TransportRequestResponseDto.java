package com.example.logistics.dto.response;

import com.example.logistics.entity.TransportRequestStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRequestResponseDto {

    private UUID transportRequestId;
    private String transportRequestCode;
    private UUID farmerId;
    private UUID orderId;
    private String auctionRefCode;
    private String productName;
    private BigDecimal quantity;
    private String quantityUnit;
    private BigDecimal estimatedDistanceKm;
    private TransportRequestStatusEnum requestStatus;
    private OffsetDateTime createdAt;

}
