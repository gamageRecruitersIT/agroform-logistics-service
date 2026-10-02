package com.example.logistics.service;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.LocationUpdateRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;
import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface DeliveryTrackingService {

    // ------------------------------------------------------------------
    // Internal (in-process) methods - called by other modules, no user check
    // ------------------------------------------------------------------

    /**
     * Creates the initial delivery_status (AWAITING_PICKUP) + first history row.
     * Chamuditha calls this right after a transport_task row is created.
     * Throws ResourceNotFoundException if the task does not exist.
     */
    DeliveryStatusResponse initializeTracking(UUID transportTaskId);

    /**
     * Dilsha calls this after the Warehouse Service responds (DELIVERED / UNLOADED_AT_WAREHOUSE).
     * Appends a WAREHOUSE_ACK history row; never changes the delivery status itself.
     */
    void recordWarehouseAcknowledgement(UUID transportTaskId, String notes);

    /**
     * Dilsha calls this when the Warehouse Service re-confirms a delivery (system-initiated, no user).
     * Only DELIVERED / UNLOADED_AT_WAREHOUSE are accepted (BadRequestException otherwise).
     *
     * @return true  if the status was moved forward by exactly one step,
     *         false if the task is already at or past confirmedStatus (nothing changed).
     * @throws com.example.logistics.exception.InvalidDeliveryStatusException if confirmedStatus would skip a step
     * @throws com.example.logistics.exception.ResourceNotFoundException      if the task has no tracking
     */
    boolean confirmStatusFromWarehouse(UUID transportTaskId, DeliveryStatusEnum confirmedStatus, String notes);

    // ------------------------------------------------------------------
    // API methods - task addressed by public transport_task_code, caller checked
    // ------------------------------------------------------------------

    /** TRANSPORTER (owner) only. */
    DeliveryStatusResponse initializeTracking(String taskCode, CurrentUser user);

    DeliveryStatusResponse getCurrentStatus(String taskCode, CurrentUser user);

    /** History, newest last. Live GPS pings are only included when includeLocations is true. */
    List<TrackingUpdateResponse> getHistory(String taskCode, boolean includeLocations, CurrentUser user);

    /** Most recent GPS position of the task. */
    TrackingUpdateResponse getLatestLocation(String taskCode, CurrentUser user);

    /** Tasks the caller is involved in (farmer's, driver's or transporter's). */
    List<DeliveryStatusResponse> getMyTasks(CurrentUser user);

    /**
     * Driver: moves the status exactly one step forward.
     * Transporter: only with authorizedOverride (backward move / skipped step).
     * Throws InvalidDeliveryStatusException on an invalid transition.
     */
    DeliveryStatusResponse updateStatus(String taskCode, TrackingStatusUpdateRequest request, CurrentUser user);

    /** Assigned driver only: live GPS ping. */
    TrackingUpdateResponse submitLocation(String taskCode, LocationUpdateRequest request, CurrentUser user);

    /** Assigned driver or owning transporter. */
    DeliveryStatusResponse flagDelay(String taskCode, DelayFlagRequest request, CurrentUser user);

    /** Owning transporter only. */
    DeliveryStatusResponse resolveDelay(String taskCode, DelayFlagRequest request, CurrentUser user);
}
