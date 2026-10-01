package com.example.logistics.event;

import com.example.logistics.entity.enums.DeliveryStatusEnum;
import com.example.logistics.repository.TransportTaskRef;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Published (inside the status-update transaction) every time a task's delivery status changes.
 *
 * Other modules listen with:
 *   @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
 * so their work (Kafka publish, warehouse Feign call, notification Feign call) only runs
 * once the status change is committed, and a failure there never rolls the status back.
 *
 *  - Gayani : map newStatus -> Kafka event (LOADED = PICKUP_STARTED, IN_TRANSIT, DELIVERED,
 *             UNLOADED_AT_WAREHOUSE) and append a logistics_event row.
 *  - Dilsha : when newStatus is DELIVERED or UNLOADED_AT_WAREHOUSE, call Warehouse Service, then
 *             DeliveryTrackingService.recordWarehouseAcknowledgement(...) with the result.
 *  - Navodya: send delivery status notification to task.farmerId() / task.transporterId().
 */
public record DeliveryStatusChangedEvent(
        TransportTaskRef task,
        DeliveryStatusEnum previousStatus,
        DeliveryStatusEnum newStatus,
        boolean delayed,
        UUID updatedBy,
        BigDecimal latitude,
        BigDecimal longitude,
        String notes,
        OffsetDateTime occurredAt) {
}
