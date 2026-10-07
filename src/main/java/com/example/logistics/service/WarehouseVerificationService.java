package com.example.logistics.service;

import com.example.logistics.dto.request.WarehouseReConfirmRequest;
import com.example.logistics.dto.response.WarehouseReConfirmResponse;
import com.example.logistics.entity.DeliveryStatusEnum;

import java.util.UUID;

/**
 * Handles outbound delivery notifications to Warehouse & Inventory Service
 * and processes inbound re-confirmation calls from that service.
 */
public interface WarehouseVerificationService {

    /**
     * Sends a delivery notification to Warehouse & Inventory Service.
     *
     * <p>This method must NOT throw or propagate exceptions — the delivery
     * status update has already been committed by the time this is called.
     * Any warehouse communication failure is logged and swallowed per spec.
     *
     * @param transportTaskId   UUID of the transport task
     * @param transportTaskCode human-readable task code (e.g. TTK-XXXX)
     * @param warehouseId       UUID of the destination warehouse
     * @param deliveryStatus    DELIVERED or UNLOADED_AT_WAREHOUSE (enum)
     * @return the acknowledgement status from the Warehouse Service, or null if the call failed
     */
    String notifyWarehouse(
            UUID               transportTaskId,
            String             transportTaskCode,
            UUID               warehouseId,
            DeliveryStatusEnum deliveryStatus
    );

    /**
     * Called by WarehouseVerificationController when Warehouse Service re-confirms a delivery.
     *
     * <p>Delegates to {@link DeliveryTrackingService#confirmStatusFromWarehouse} which moves the
     * delivery status to the confirmed value if it has not yet reached it, or does nothing if it
     * is already at or past that point (idempotent).
     *
     * <p>Only DELIVERED and UNLOADED_AT_WAREHOUSE are accepted as confirmedStatus values.
     *
     * @throws com.example.logistics.exception.BadRequestException      if confirmedStatus is invalid
     * @throws com.example.logistics.exception.ResourceNotFoundException if the task does not exist
     */
    WarehouseReConfirmResponse handleWarehouseReConfirmation(WarehouseReConfirmRequest request);
}
