package com.example.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Task + request snapshot returned to Warehouse & Inventory Service.
 * Field names must match Warehouse's LogisticsTaskDetailResponse exactly.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportTaskDetailResponse {

    // TransportTask
    private UUID transportTaskId;
    private String transportTaskCode;
    private UUID transportRequestId;
    private UUID transporterId;
    private UUID driverId;

    // TransportRequest / Order
    private UUID orderId;
    private UUID farmerId;
    private UUID buyerId;
    private String auctionRefCode;
    private String productName;
    private BigDecimal quantityKg;

    // Name snapshots (null until Identity Service exposes a name lookup)
    private String farmerName;
    private String transporterName;
    private String driverName;
}
