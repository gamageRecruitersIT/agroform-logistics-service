package com.example.logistics.service.impl;

import com.example.logistics.dto.request.WarehouseReConfirmRequest;
import com.example.logistics.dto.response.WarehouseReConfirmResponse;
import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.feign.client.WarehouseServiceFeignClient;
import com.example.logistics.feign.dto.WarehouseDeliveryNotificationRequest;
import com.example.logistics.feign.dto.WarehouseDeliveryNotificationResponse;
import com.example.logistics.service.DeliveryTrackingService;
import com.example.logistics.service.WarehouseVerificationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles outbound delivery notifications to Warehouse & Inventory Service
 * and processes inbound re-confirmation calls from that service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseVerificationServiceImpl implements WarehouseVerificationService {

    private final WarehouseServiceFeignClient warehouseServiceFeignClient;
    private final DeliveryTrackingService     deliveryTrackingService;

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
    @Override
    public String notifyWarehouse(
            UUID               transportTaskId,
            String             transportTaskCode,
            UUID               warehouseId,
            DeliveryStatusEnum deliveryStatus
    ) {
        log.info("[WarehouseVerification] Notifying warehouse for task={} status={}",
                transportTaskCode, deliveryStatus);

        WarehouseDeliveryNotificationRequest request =
                new WarehouseDeliveryNotificationRequest(
                        transportTaskId,
                        transportTaskCode,
                        warehouseId,
                        deliveryStatus.name(),   // send as string to Warehouse Service
                        Instant.now()
                );

        try {
            WarehouseDeliveryNotificationResponse response =
                    warehouseServiceFeignClient.notifyDeliveryArrival(request);

            String ack = response != null ? response.getAcknowledgementStatus() : null;
            log.info("[WarehouseVerification] Warehouse acknowledged task={} ack={}",
                    transportTaskCode, ack);
            return ack != null ? ack : "ACKNOWLEDGED";

        } catch (FeignException ex) {
            // Per spec: log the failure, do NOT propagate.
            // Delivery status is already committed — warehouse failure must not roll it back.
            log.error(
                    "[WarehouseVerification] Feign call failed for task={} status={} httpStatus={} reason={}",
                    transportTaskCode, deliveryStatus, ex.status(), ex.getMessage()
            );
            return null;
        } catch (Exception ex) {
            log.error(
                    "[WarehouseVerification] Unexpected error notifying warehouse for task={} : {}",
                    transportTaskCode, ex.getMessage(), ex
            );
            return null;
        }
    }

    /**
     * Called by WarehouseVerificationController when Warehouse Service re-confirms a delivery.
     *
     * <p>Only DELIVERED and UNLOADED_AT_WAREHOUSE are accepted as confirmedStatus values.
     */
    @Override
    public WarehouseReConfirmResponse handleWarehouseReConfirmation(
            WarehouseReConfirmRequest request
    ) {
        UUID   taskId          = request.getTransportTaskId();
        String confirmedString = request.getConfirmedStatus();

        log.info("[WarehouseVerification] Re-confirmation received for task={} confirmedStatus={}",
                taskId, confirmedString);

        // ── Parse and validate the confirmed status ───────────────────────────
        DeliveryStatusEnum confirmedStatus;
        try {
            confirmedStatus = DeliveryStatusEnum.valueOf(confirmedString);
        } catch (IllegalArgumentException ex) {
            log.warn("[WarehouseVerification] Unrecognized confirmedStatus={} for task={}",
                    confirmedString, taskId);
            throw new BadRequestException(
                    "Unrecognized confirmedStatus: " + confirmedString
            );
        }

        if (confirmedStatus != DeliveryStatusEnum.DELIVERED
                && confirmedStatus != DeliveryStatusEnum.UNLOADED_AT_WAREHOUSE) {
            log.warn("[WarehouseVerification] Unexpected confirmedStatus={} for task={}",
                    confirmedStatus, taskId);
            throw new BadRequestException(
                    "confirmedStatus must be DELIVERED or UNLOADED_AT_WAREHOUSE, got: " + confirmedStatus
            );
        }

        // ── Confirm delivery status via DeliveryTrackingService ──────────────
        // confirmStatusFromWarehouse moves the status forward if not yet reached,
        // or returns false (no-op) if already at or past the confirmed value.
        try {
            String notes = request.getNotes() != null
                    ? "Re-confirmed by Warehouse Service: " + request.getNotes()
                    : "Re-confirmed by Warehouse Service";

            boolean moved = deliveryTrackingService.confirmStatusFromWarehouse(taskId, confirmedStatus, notes);
            if (moved) {
                log.info("[WarehouseVerification] Delivery status synced for task={} to {}",
                        taskId, confirmedStatus);
                return WarehouseReConfirmResponse.accepted(taskId, confirmedString);
            }
            log.info("[WarehouseVerification] Status already at or past confirmed value for task={}", taskId);
            return WarehouseReConfirmResponse.alreadyConfirmed(taskId, confirmedString);

        } catch (ResourceNotFoundException ex) {
            log.error("[WarehouseVerification] Transport task not found for re-confirmation: task={}",
                    taskId);
            throw ex;  // 404 — the task UUID the Warehouse Service sent does not exist
        }
    }
}
