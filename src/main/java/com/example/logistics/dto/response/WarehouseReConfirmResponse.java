package com.example.logistics.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned to Warehouse & Inventory Service after processing
 * a re-confirmation request on:
 *   POST /api/v1/logistics/internal/warehouse-verification
 */
public class WarehouseReConfirmResponse {

    private UUID   transportTaskId;

    /** Current delivery status in Logistics Service after processing the re-confirmation. */
    private String recordedStatus;

    /** Outcome from Logistics Service side. Values: "ACCEPTED" | "ALREADY_CONFIRMED" | "REJECTED" */
    private String outcome;

    private String  message;
    private Instant processedAt;

    public WarehouseReConfirmResponse() {}

    public WarehouseReConfirmResponse(
            UUID transportTaskId,
            String recordedStatus,
            String outcome,
            String message,
            Instant processedAt
    ) {
        this.transportTaskId = transportTaskId;
        this.recordedStatus  = recordedStatus;
        this.outcome         = outcome;
        this.message         = message;
        this.processedAt     = processedAt;
    }

    public static WarehouseReConfirmResponse accepted(UUID taskId, String status) {
        return new WarehouseReConfirmResponse(
                taskId, status, "ACCEPTED",
                "Delivery re-confirmation recorded successfully.",
                Instant.now()
        );
    }

    public static WarehouseReConfirmResponse alreadyConfirmed(UUID taskId, String status) {
        return new WarehouseReConfirmResponse(
                taskId, status, "ALREADY_CONFIRMED",
                "Delivery was already confirmed at this status.",
                Instant.now()
        );
    }

    public UUID getTransportTaskId() { return transportTaskId; }
    public void setTransportTaskId(UUID transportTaskId) { this.transportTaskId = transportTaskId; }

    public String getRecordedStatus() { return recordedStatus; }
    public void setRecordedStatus(String recordedStatus) { this.recordedStatus = recordedStatus; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
