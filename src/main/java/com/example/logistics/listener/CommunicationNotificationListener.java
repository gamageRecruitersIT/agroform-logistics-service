package com.example.logistics.listener;

import com.example.logistics.event.DeliveryDelayedEvent;
import com.example.logistics.event.DeliveryStatusChangedEvent;
import com.example.logistics.feign.client.CommunicationClient;
import com.example.logistics.feign.dto.DelayNotificationRequest;
import com.example.logistics.feign.dto.DeliveryStatusNotificationRequest;
import com.example.logistics.repository.TransportTaskRef;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Connects Dilum's tracking events to Navodya's Communication & Support Service client.
 *
 * AFTER_COMMIT: the status change / delay flag is already saved. Every call is wrapped so a
 * notification failure is logged and never reaches the caller (a thrown exception from an
 * AFTER_COMMIT listener would otherwise turn a successful update into an HTTP error).
 * The assignment notification is NOT sent here - it belongs to the assignment module (Chamuditha).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommunicationNotificationListener {

    private final CommunicationClient communicationClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(DeliveryStatusChangedEvent event) {
        TransportTaskRef task = event.task();
        try {
            communicationClient.sendDeliveryStatusNotification(new DeliveryStatusNotificationRequest(
                    task.transportRequestCode(),
                    task.transportTaskCode(),
                    task.driverId(),
                    task.farmerId(),
                    event.newStatus().name(),
                    event.occurredAt().toInstant()));
        } catch (Exception ex) {
            log.warn("Delivery status notification failed for task {} ({}): {}",
                    task.transportTaskCode(), event.newStatus(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDelayed(DeliveryDelayedEvent event) {
        TransportTaskRef task = event.task();
        try {
            communicationClient.sendDelayNotification(new DelayNotificationRequest(
                    task.transportRequestCode(),
                    task.transportTaskCode(),
                    task.driverId(),
                    task.farmerId(),
                    event.reason(),
                    null,                                   // the delay flag has no estimated minutes
                    event.occurredAt().toInstant()));
        } catch (Exception ex) {
            log.warn("Delay notification failed for task {}: {}", task.transportTaskCode(), ex.getMessage());
        }
    }
}
