package com.example.logistics.service.impl;

import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.event.DeliveryStatusChangedEvent;
import com.example.logistics.service.LogisticsWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Converts committed delivery status changes into logistics audit events.
 */
@Component
@RequiredArgsConstructor
public class LogisticsDeliveryEventListener {

    private final LogisticsWorkflowService logisticsWorkflowService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        DeliveryStatusEnum status = event.newStatus();
        String transportRequestCode = event.task().transportRequestCode();
        String taskCode = event.task().transportTaskCode();
        String vehicleCode = event.task().vehicleCode();
        String createdBy = event.updatedBy() == null ? null : event.updatedBy().toString();

        switch (status) {
            case LOADED -> logisticsWorkflowService.onPickupStarted(
                    transportRequestCode, taskCode, vehicleCode, createdBy);
            case IN_TRANSIT -> logisticsWorkflowService.onInTransit(
                    transportRequestCode, taskCode, vehicleCode, createdBy);
            case DELIVERED -> logisticsWorkflowService.onDelivered(
                    transportRequestCode, taskCode, vehicleCode, createdBy);
            case UNLOADED_AT_WAREHOUSE -> logisticsWorkflowService.onUnloadedAtWarehouse(
                    transportRequestCode, taskCode, vehicleCode, createdBy);
            case AWAITING_PICKUP -> {
                // Initial tracking state has no lifecycle event in Gayani's event list.
            }
        }
    }
}