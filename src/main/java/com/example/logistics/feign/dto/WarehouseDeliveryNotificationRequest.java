package com.example.logistics.feign.dto;

import java.time.Instant;
import java.util.UUID;

/** Payload sent to Warehouse & Inventory Service when a delivery is marked DELIVERED or UNLOADED_AT_WAREHOUSE. */
public class WarehouseDeliveryNotificationRequest {

    private UUID   transportTaskId;
    private String transportTaskCode;
    private UUID   warehouseId;

    /** The delivery status that triggered this notification. e.g. "DELIVERED" | "UNLOADED_AT_WAREHOUSE" */
    private String deliveryStatus;

    private Instant occurredAt;

    public WarehouseDeliveryNotificationRequest() {}

    public WarehouseDeliveryNotificationRequest(
            UUID transportTaskId,
            String transportTaskCode,
            UUID warehouseId,
            String deliveryStatus,
            Instant occurredAt
    ) {
        this.transportTaskId   = transportTaskId;
        this.transportTaskCode = transportTaskCode;
        this.warehouseId       = warehouseId;
        this.deliveryStatus    = deliveryStatus;
        this.occurredAt        = occurredAt;
    }

    public UUID getTransportTaskId() { return transportTaskId; }
    public void setTransportTaskId(UUID transportTaskId) { this.transportTaskId = transportTaskId; }

    public String getTransportTaskCode() { return transportTaskCode; }
    public void setTransportTaskCode(String transportTaskCode) { this.transportTaskCode = transportTaskCode; }

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
