package com.example.logistics.service;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;

import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingService {

    /**
     * Creates the initial delivery_status row (AWAITING_PICKUP) and the first
     * tracking_update history row for a newly created transport task.
     * Called by Chamuditha's Driver Assignment & Task Management module right
     * after a transport task is created.
     */
    DeliveryStatusResponse initializeTracking(UUID transportTaskId);

    DeliveryStatusResponse getCurrentStatus(UUID transportTaskId);

    List<TrackingUpdateResponse> getHistory(UUID transportTaskId);

    /**
     * Moves the delivery status forward. Throws InvalidDeliveryStatusException
     * when the requested status skips a step or moves backward and
     * request.authorizedOverride is not set.
     */
    DeliveryStatusResponse updateStatus(UUID transportTaskId, TrackingStatusUpdateRequest request);

    DeliveryStatusResponse flagDelay(UUID transportTaskId, DelayFlagRequest request);

    DeliveryStatusResponse resolveDelay(UUID transportTaskId, DelayFlagRequest request);
}
