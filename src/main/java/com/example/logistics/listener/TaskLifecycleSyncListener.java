package com.example.logistics.listener;

import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.event.DeliveryStatusChangedEvent;
import com.example.logistics.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Keeps the other modules' tables consistent with the delivery status, in the SAME
 * transaction as the status update (BEFORE_COMMIT) so it is all-or-nothing:
 *
 *   LOADED (first move)        -> transport_task.task_status = IN_PROGRESS
 *   UNLOADED_AT_WAREHOUSE      -> transport_task.task_status = COMPLETED
 *                                 driver_assignment released (driver can take a new task)
 *                                 vehicle freed via VehicleService.releaseVehicleAfterTask(vehicleCode)
 *                                 (Vasitha's module: ASSIGNED -> AVAILABLE only)
 *                                 transport_request.request_status = COMPLETED
 *
 * This is a stop-gap so the flow works end to end. When Chamuditha (task/driver),
 * Vasitha (vehicle) and Dinujaya (request) add their own completion handling, turn this off with
 *   logistics.tracking.sync-task-lifecycle=false
 * so the same rows are not updated twice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "logistics.tracking", name = "sync-task-lifecycle",
        havingValue = "true", matchIfMissing = true)
public class TaskLifecycleSyncListener {

    private final JdbcTemplate jdbc;
    private final VehicleService vehicleService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onStatusChanged(DeliveryStatusChangedEvent event) {
        UUID taskId = event.task().transportTaskId();
        DeliveryStatusEnum status = event.newStatus();

        if (status == DeliveryStatusEnum.LOADED) {
            jdbc.update("""
                    UPDATE transport_task
                       SET task_status = 'IN_PROGRESS'::transport_task_status_enum
                     WHERE transport_task_id = ?
                       AND task_status = 'ASSIGNED'::transport_task_status_enum
                    """, taskId);
            log.info("Task {} marked IN_PROGRESS", event.task().transportTaskCode());
            return;
        }

        if (status == DeliveryStatusEnum.UNLOADED_AT_WAREHOUSE) {
            jdbc.update("""
                    UPDATE transport_task
                       SET task_status = 'COMPLETED'::transport_task_status_enum
                     WHERE transport_task_id = ?
                    """, taskId);

            jdbc.update("""
                    UPDATE driver_assignment
                       SET is_active = FALSE, released_at = NOW()
                     WHERE transport_task_id = ? AND is_active = TRUE
                    """, taskId);

            vehicleService.releaseVehicleAfterTask(event.task().vehicleCode());

            jdbc.update("""
                    UPDATE transport_request
                       SET request_status = 'COMPLETED'::transport_request_status_enum
                     WHERE transport_request_id = ?
                    """, event.task().transportRequestId());

            log.info("Task {} completed: driver released, vehicle freed, request completed",
                    event.task().transportTaskCode());
        }
    }
}
