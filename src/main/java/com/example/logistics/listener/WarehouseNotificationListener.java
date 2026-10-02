package com.example.logistics.listener;

import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.event.DeliveryStatusChangedEvent;
import com.example.logistics.service.DeliveryTrackingService;
import com.example.logistics.service.WarehouseVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Connects Dilum's tracking events to Dilsha's warehouse verification:
 * once a delivery is DELIVERED / UNLOADED_AT_WAREHOUSE (and committed), call the Warehouse Service
 * and store its acknowledgement as a WAREHOUSE_ACK history row.
 *
 * AFTER_COMMIT: a warehouse failure can never roll back the status change.
 * REQUIRES_NEW: the ack row is written in its own transaction (the original one is already finished).
 * Status changes that came FROM the warehouse (updatedBy == null) are skipped to avoid a notify loop.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarehouseNotificationListener {

    private final WarehouseVerificationService warehouseVerificationService;
    private final DeliveryTrackingService deliveryTrackingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(DeliveryStatusChangedEvent event) {
        DeliveryStatusEnum status = event.newStatus();
        if (status != DeliveryStatusEnum.DELIVERED && status != DeliveryStatusEnum.UNLOADED_AT_WAREHOUSE) {
            return;
        }
        if (event.updatedBy() == null) {
            return; // change was confirmed by the warehouse itself
        }
        if (event.task().warehouseId() == null) {
            log.warn("Task {} has no warehouse_id on its transport request; warehouse not notified",
                    event.task().transportTaskCode());
            return;
        }

        String ack = warehouseVerificationService.notifyWarehouse(
                event.task().transportTaskId(),
                event.task().transportTaskCode(),
                event.task().warehouseId(),
                status);

        if (ack != null) {
            try {
                deliveryTrackingService.recordWarehouseAcknowledgement(
                        event.task().transportTaskId(), "Warehouse acknowledged " + status + ": " + ack);
            } catch (RuntimeException ex) {
                log.error("Could not store warehouse acknowledgement for task {}: {}",
                        event.task().transportTaskCode(), ex.getMessage());
            }
        }
    }
}
