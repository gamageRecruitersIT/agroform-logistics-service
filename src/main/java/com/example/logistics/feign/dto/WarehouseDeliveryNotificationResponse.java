package com.example.logistics.feign.dto;

/**
 * Response received from Warehouse & Inventory Service after a delivery notification.
 *
 * ⚠️ TODO: confirm the actual response contract with the Warehouse team.
 * Expected acknowledgementStatus values: RECEIVED | REJECTED | PENDING
 */
public class WarehouseDeliveryNotificationResponse {

    private String acknowledgementStatus;

    /** Optional message from Warehouse explaining the result. */
    private String message;

    public WarehouseDeliveryNotificationResponse() {}

    public WarehouseDeliveryNotificationResponse(String acknowledgementStatus, String message) {
        this.acknowledgementStatus = acknowledgementStatus;
        this.message               = message;
    }

    public String getAcknowledgementStatus() { return acknowledgementStatus; }
    public void setAcknowledgementStatus(String acknowledgementStatus) {
        this.acknowledgementStatus = acknowledgementStatus;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
