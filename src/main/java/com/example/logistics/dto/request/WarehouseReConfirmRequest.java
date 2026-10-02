package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request body received from the Warehouse & Inventory Service on the internal
 * re-confirmation endpoint:
 *   POST /api/v1/logistics/internal/warehouse-verification
 *
 * Warehouse Service calls this endpoint to re-confirm that a delivery has been
 * physically received at their facility.
 */
public class WarehouseReConfirmRequest {

    /**
     * The transport task whose delivery is being re-confirmed.
     * Logistics Service uses this to look up and update delivery status.
     */
    @NotNull(message = "transportTaskId must not be null")
    private UUID transportTaskId;

    /** The warehouse that is confirming receipt. */
    @NotNull(message = "warehouseId must not be null")
    private UUID warehouseId;

    /** The status the warehouse is confirming.
     * Expected: "DELIVERED" | "UNLOADED_AT_WAREHOUSE" */
    @NotNull(message = "confirmedStatus must not be null")
    private String confirmedStatus;

    /** Optional notes from the warehouse (e.g. quantity mismatch, damage remarks). */
    private String notes;

    public WarehouseReConfirmRequest() {}

    public UUID getTransportTaskId() { return transportTaskId; }
    public void setTransportTaskId(UUID transportTaskId) { this.transportTaskId = transportTaskId; }

    public UUID getWarehouseId() { return warehouseId; }
    public void setWarehouseId(UUID warehouseId) { this.warehouseId = warehouseId; }

    public String getConfirmedStatus() { return confirmedStatus; }
    public void setConfirmedStatus(String confirmedStatus) { this.confirmedStatus = confirmedStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
