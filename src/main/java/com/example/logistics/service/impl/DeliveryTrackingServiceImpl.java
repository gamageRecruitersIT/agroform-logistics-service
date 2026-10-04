package com.example.logistics.service.impl;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.LocationUpdateRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;
import com.example.logistics.entity.DeliveryStatus;
import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.entity.TrackingUpdate;
import com.example.logistics.entity.enums.TrackingUpdateType;
import com.example.logistics.event.DeliveryDelayResolvedEvent;
import com.example.logistics.event.DeliveryDelayedEvent;
import com.example.logistics.event.DeliveryStatusChangedEvent;
import com.example.logistics.exception.*;
import com.example.logistics.repository.DeliveryStatusRepository;
import com.example.logistics.repository.TrackingUpdateRepository;
import com.example.logistics.repository.TransportTaskLookupRepository;
import com.example.logistics.repository.TransportTaskRef;
import com.example.logistics.security.CurrentUser;
import com.example.logistics.security.UserRole;
import com.example.logistics.service.DeliveryTrackingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {

    private final DeliveryStatusRepository deliveryStatusRepository;
    private final TrackingUpdateRepository trackingUpdateRepository;
    private final TransportTaskLookupRepository taskLookup;
    private final ApplicationEventPublisher eventPublisher;

    /** transport_task_code is VARCHAR(20), generated as TTK-XXXXXXXX. */
    private static final Pattern TASK_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9-]{1,20}$");
    private static final int MIN_REASON_LENGTH = 3;

    @PersistenceContext
    private EntityManager entityManager;

    // ==================================================================
    // Internal methods (other modules)
    // ==================================================================

    @Override
    @Transactional
    public DeliveryStatusResponse initializeTracking(UUID transportTaskId) {
        if (transportTaskId == null) {
            throw new BadRequestException("transportTaskId is required");
        }
        // The assignment module saves the task with JPA in the same transaction, but the lookup below is raw SQL.
        // Push the pending inserts to the database first so the task row is visible to it.
        entityManager.flush();
        TransportTaskRef task = taskLookup.findById(transportTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Transport task not found: " + transportTaskId));
        return initialize(task);
    }

    @Override
    @Transactional
    public void recordWarehouseAcknowledgement(UUID transportTaskId, String notes) {
        DeliveryStatus status = deliveryStatusRepository.findByTransportTaskId(transportTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No delivery tracking found for transport task " + transportTaskId));

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(transportTaskId)
                .updateType(TrackingUpdateType.WAREHOUSE_ACK)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .notes(notes)
                .build());
        log.info("Warehouse acknowledgement recorded for transport task {}", transportTaskId);
    }

    @Override
    @Transactional
    public boolean confirmStatusFromWarehouse(UUID transportTaskId, DeliveryStatusEnum confirmedStatus, String notes) {
        if (confirmedStatus != DeliveryStatusEnum.DELIVERED
                && confirmedStatus != DeliveryStatusEnum.UNLOADED_AT_WAREHOUSE) {
            throw new BadRequestException(
                    "Warehouse can only confirm DELIVERED or UNLOADED_AT_WAREHOUSE, got " + confirmedStatus);
        }
        TransportTaskRef task = taskLookup.findById(transportTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Transport task not found: " + transportTaskId));
        requireRequestOpen(task);
        DeliveryStatus status = findStatusForUpdate(task);
        DeliveryStatusEnum previous = status.getCurrentStatus();

        if (!previous.isBackwardOrSameAs(confirmedStatus)) {
            // task is already past (or at) the confirmed status - nothing to do
            return false;
        }
        if (!previous.isValidForwardStepTo(confirmedStatus)) {
            throw new InvalidDeliveryStatusException(
                    "Cannot skip delivery status from " + previous + " to " + confirmedStatus
                            + "; status must progress one step at a time");
        }

        status.setCurrentStatus(confirmedStatus);
        status.setUpdatedBy(null);
        if (notes != null) {
            status.setNotes(notes);
        }
        status = deliveryStatusRepository.save(status);

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.STATUS_CHANGE)
                .previousStatus(previous)
                .newStatus(confirmedStatus)
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .notes(notes)
                .build());

        log.info("Transport task {} delivery status: {} -> {} (confirmed by Warehouse Service)",
                task.transportTaskCode(), previous, confirmedStatus);

        // updatedBy == null marks a system/warehouse-initiated change (the warehouse listener skips these)
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(
                task, previous, confirmedStatus, status.isDelayed(), null,
                null, null, notes, OffsetDateTime.now()));
        return true;
    }

    // ==================================================================
    // API methods
    // ==================================================================

    @Override
    @Transactional
    public DeliveryStatusResponse initializeTracking(String taskCode, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        if (user.role() != UserRole.TRANSPORTER) {
            throw new UnauthorizedRoleException("Only the transporter can initialize delivery tracking");
        }
        if (!user.userId().equals(task.transporterId())) {
            throw new ForbiddenException("You are not the transporter of task " + task.transportTaskCode());
        }
        return initialize(task);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryStatusResponse getCurrentStatus(String taskCode, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        requireViewAccess(task, user);
        return toDeliveryStatusResponse(findStatus(task), task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackingUpdateResponse> getHistory(String taskCode, boolean includeLocations, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        requireViewAccess(task, user);
        findStatus(task); // 404 if tracking was never initialized

        List<TrackingUpdate> rows = includeLocations
                ? trackingUpdateRepository.findByTransportTaskIdOrderByRecordedAtAsc(task.transportTaskId())
                : trackingUpdateRepository.findByTransportTaskIdAndUpdateTypeNotOrderByRecordedAtAsc(
                task.transportTaskId(), TrackingUpdateType.LOCATION);
        return rows.stream().map(r -> toTrackingUpdateResponse(r, task)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrackingUpdateResponse getLatestLocation(String taskCode, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        requireViewAccess(task, user);
        findStatus(task);

        return trackingUpdateRepository
                .findFirstByTransportTaskIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByRecordedAtDesc(
                        task.transportTaskId())
                .map(r -> toTrackingUpdateResponse(r, task))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No location recorded yet for task " + task.transportTaskCode()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryStatusResponse> getMyTasks(CurrentUser user) {
        List<TransportTaskRef> tasks = switch (user.role()) {
            case FARMER -> taskLookup.findByFarmer(user.userId());
            case DRIVER -> taskLookup.findByDriver(user.userId());
            case TRANSPORTER -> taskLookup.findByTransporter(user.userId());
        };
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<UUID, DeliveryStatus> statusByTask = deliveryStatusRepository
                .findByTransportTaskIdIn(tasks.stream().map(TransportTaskRef::transportTaskId).toList())
                .stream()
                .collect(Collectors.toMap(DeliveryStatus::getTransportTaskId, Function.identity()));

        return tasks.stream()
                .filter(t -> statusByTask.containsKey(t.transportTaskId()))
                .map(t -> toDeliveryStatusResponse(statusByTask.get(t.transportTaskId()), t))
                .toList();
    }

    @Override
    @Transactional
    public DeliveryStatusResponse updateStatus(String taskCode, TrackingStatusUpdateRequest request, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        boolean override = request.isAuthorizedOverride();
        authorizeStatusChange(task, user, override);
        requireRequestOpen(task);
        requireValidCoordinates(request.getLatitude(), request.getLongitude());

        String notes = normalizeText(request.getNotes());
        if (override && notes == null) {
            throw new BadRequestException(
                    "notes (the reason for the correction) are required when authorizedOverride is true");
        }

        DeliveryStatus status = findStatusForUpdate(task);
        DeliveryStatusEnum previous = status.getCurrentStatus();
        DeliveryStatusEnum next = request.getNewStatus();

        // The task is completed (driver released, vehicle freed, request COMPLETED) once it is unloaded;
        // not even an authorized override may reopen it.
        if (previous.isTerminal()) {
            throw new InvalidDeliveryStatusException(
                    "Transport task " + task.transportTaskCode() + " is already " + previous
                            + "; a completed delivery can no longer be changed");
        }
        if (previous == next) {
            throw new BadRequestException(
                    "Transport task " + task.transportTaskCode() + " is already in status " + previous);
        }

        if (!override) {
            if (previous.isBackwardOrSameAs(next)) {
                throw new InvalidDeliveryStatusException(
                        "Cannot move delivery status backward from " + previous + " to " + next
                                + " without an authorized override");
            }
            if (!previous.isValidForwardStepTo(next)) {
                throw new InvalidDeliveryStatusException(
                        "Cannot skip delivery status from " + previous + " to " + next
                                + "; status must progress one step at a time");
            }
        }

        status.setCurrentStatus(next);
        status.setUpdatedBy(user.userId());
        if (notes != null) {
            status.setNotes(notes);
        }
        status = deliveryStatusRepository.save(status);

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.STATUS_CHANGE)
                .previousStatus(previous)
                .newStatus(next)
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedBy(user.userId())
                .notes(notes)
                .build());

        log.info("Transport task {} delivery status: {} -> {} (by {})",
                task.transportTaskCode(), previous, next, user.role());

        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(
                task, previous, next, status.isDelayed(), user.userId(),
                request.getLatitude(), request.getLongitude(), notes, OffsetDateTime.now()));

        return toDeliveryStatusResponse(status, task);
    }

    @Override
    @Transactional
    public TrackingUpdateResponse submitLocation(String taskCode, LocationUpdateRequest request, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);

        if (user.role() != UserRole.DRIVER) {
            throw new UnauthorizedRoleException("Only the assigned driver can submit live location updates");
        }
        if (!user.userId().equals(task.driverId())) {
            throw new ForbiddenException("You are not the assigned driver of task " + task.transportTaskCode());
        }
        requireRequestOpen(task);
        requireValidCoordinates(request.getLatitude(), request.getLongitude());

        DeliveryStatus status = findStatus(task);
        if (status.getCurrentStatus().isTerminal()) {
            throw new InvalidDeliveryStatusException(
                    "Cannot submit a location for task " + task.transportTaskCode()
                            + "; delivery already reached " + status.getCurrentStatus());
        }

        TrackingUpdate saved = trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.LOCATION)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedBy(user.userId())
                .notes(normalizeText(request.getNotes()))
                .build());

        return toTrackingUpdateResponse(saved, task);
    }

    @Override
    @Transactional
    public DeliveryStatusResponse flagDelay(String taskCode, DelayFlagRequest request, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);
        requireDriverOrTransporterOfTask(task, user, "flag a delay");
        requireRequestOpen(task);
        String reason = requireReason(request.getReason());

        DeliveryStatus status = findStatusForUpdate(task);

        if (status.getCurrentStatus().isTerminal()) {
            throw new InvalidDeliveryStatusException(
                    "Cannot flag a delay on transport task " + task.transportTaskCode()
                            + "; delivery already reached " + status.getCurrentStatus());
        }
        if (status.isDelayed()) {
            throw new BadRequestException(
                    "Transport task " + task.transportTaskCode() + " is already flagged as delayed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        status.setDelayed(true);
        status.setDelayReason(reason);
        status.setDelayedAt(now);
        status.setUpdatedBy(user.userId());
        status = deliveryStatusRepository.save(status);

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.DELAY_FLAGGED)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(true)
                .delayReason(reason)
                .updatedBy(user.userId())
                .notes("Delay flagged: " + reason)
                .build());

        log.warn("Transport task {} flagged DELAYED: {}", task.transportTaskCode(), reason);

        eventPublisher.publishEvent(new DeliveryDelayedEvent(
                task, status.getCurrentStatus(), reason, user.userId(), now));

        return toDeliveryStatusResponse(status, task);
    }

    @Override
    @Transactional
    public DeliveryStatusResponse resolveDelay(String taskCode, DelayFlagRequest request, CurrentUser user) {
        TransportTaskRef task = findTask(taskCode);

        if (user.role() != UserRole.TRANSPORTER) {
            throw new UnauthorizedRoleException("Only the transporter can resolve a delay");
        }
        if (!user.userId().equals(task.transporterId())) {
            throw new ForbiddenException("You are not the transporter of task " + task.transportTaskCode());
        }
        String resolution = requireReason(request.getReason());

        DeliveryStatus status = findStatusForUpdate(task);
        if (!status.isDelayed()) {
            throw new BadRequestException(
                    "Transport task " + task.transportTaskCode() + " is not currently flagged as delayed");
        }

        status.setDelayed(false);
        status.setDelayReason(null);
        status.setDelayedAt(null);
        status.setUpdatedBy(user.userId());
        status = deliveryStatusRepository.save(status);

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.DELAY_RESOLVED)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(false)
                .updatedBy(user.userId())
                .notes("Delay resolved: " + resolution)
                .build());

        log.info("Transport task {} delay resolved", task.transportTaskCode());

        eventPublisher.publishEvent(new DeliveryDelayResolvedEvent(
                task, status.getCurrentStatus(), user.userId(), resolution, OffsetDateTime.now()));

        return toDeliveryStatusResponse(status, task);
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private DeliveryStatusResponse initialize(TransportTaskRef task) {
        requireRequestOpen(task);
        if (deliveryStatusRepository.existsByTransportTaskId(task.transportTaskId())) {
            throw new DuplicateResourceException(
                    "Delivery tracking already initialized for transport task " + task.transportTaskCode());
        }

        DeliveryStatus status = deliveryStatusRepository.save(DeliveryStatus.builder()
                .transportTaskId(task.transportTaskId())
                .currentStatus(DeliveryStatusEnum.AWAITING_PICKUP)
                .delayed(false)
                .build());

        trackingUpdateRepository.save(TrackingUpdate.builder()
                .transportTaskId(task.transportTaskId())
                .updateType(TrackingUpdateType.INITIALIZED)
                .previousStatus(null)
                .newStatus(DeliveryStatusEnum.AWAITING_PICKUP)
                .delayed(false)
                .notes("Tracking initialized on transport task creation")
                .build());

        log.info("Initialized delivery tracking for transport task {}", task.transportTaskCode());
        return toDeliveryStatusResponse(status, task);
    }

    private TransportTaskRef findTask(String taskCode) {
        if (taskCode == null || taskCode.isBlank()) {
            throw new BadRequestException("Transport task code is required");
        }
        String code = taskCode.trim();
        if (!TASK_CODE_PATTERN.matcher(code).matches()) {
            throw new BadRequestException("Invalid transport task code format");
        }
        return taskLookup.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Transport task not found: " + code));
    }

    private DeliveryStatus findStatus(TransportTaskRef task) {
        return deliveryStatusRepository.findByTransportTaskId(task.transportTaskId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No delivery tracking found for transport task " + task.transportTaskCode()));
    }

    private DeliveryStatus findStatusForUpdate(TransportTaskRef task) {
        return deliveryStatusRepository.findByTransportTaskIdForUpdate(task.transportTaskId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No delivery tracking found for transport task " + task.transportTaskCode()));
    }

    /** Farmer / transporter / driver may only see tasks they are part of. */
    private void requireViewAccess(TransportTaskRef task, CurrentUser user) {
        boolean allowed = switch (user.role()) {
            case FARMER -> user.userId().equals(task.farmerId());
            case TRANSPORTER -> user.userId().equals(task.transporterId());
            case DRIVER -> user.userId().equals(task.driverId());
        };
        if (!allowed) {
            throw new ForbiddenException("You do not have access to transport task " + task.transportTaskCode());
        }
    }

    private void requireDriverOrTransporterOfTask(TransportTaskRef task, CurrentUser user, String action) {
        if (user.role() == UserRole.FARMER) {
            throw new UnauthorizedRoleException("Farmers cannot " + action);
        }
        UUID expected = user.role() == UserRole.DRIVER ? task.driverId() : task.transporterId();
        if (!user.userId().equals(expected)) {
            throw new ForbiddenException("You are not assigned to task " + task.transportTaskCode());
        }
    }

    private void authorizeStatusChange(TransportTaskRef task, CurrentUser user, boolean override) {
        requireDriverOrTransporterOfTask(task, user, "update the delivery status");
        if (user.role() == UserRole.DRIVER && override) {
            throw new ForbiddenException("Only the transporter can use an authorized override");
        }
        if (user.role() == UserRole.TRANSPORTER && !override) {
            throw new ForbiddenException(
                    "Transporters can only change the status with authorizedOverride=true; "
                            + "normal step-by-step updates are submitted by the driver");
        }
    }

    /** Tracking is frozen once the parent transport request is CANCELLED or REJECTED. */
    private void requireRequestOpen(TransportTaskRef task) {
        if (task.isRequestClosed()) {
            throw new InvalidDeliveryStatusException(
                    "Transport request of task " + task.transportTaskCode() + " is "
                            + task.requestStatus() + "; delivery tracking can no longer be changed");
        }
    }

    /** Both or neither coordinate, and not the 0,0 "null island" value devices report without a GPS fix. */
    private void requireValidCoordinates(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new BadRequestException("latitude and longitude must be provided together");
        }
        if (latitude != null && latitude.signum() == 0 && longitude.signum() == 0) {
            throw new BadRequestException("Coordinates 0,0 are not a valid location; check the device GPS");
        }
    }

    /** Trims; blank becomes null so whitespace-only notes are not stored. */
    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireReason(String reason) {
        String trimmed = normalizeText(reason);
        if (trimmed == null || trimmed.length() < MIN_REASON_LENGTH) {
            throw new BadRequestException("reason must be at least " + MIN_REASON_LENGTH + " characters");
        }
        return trimmed;
    }

    private DeliveryStatusResponse toDeliveryStatusResponse(DeliveryStatus status, TransportTaskRef task) {
        return DeliveryStatusResponse.builder()
                .transportTaskCode(task.transportTaskCode())
                .transportRequestCode(task.transportRequestCode())
                .vehicleCode(task.vehicleCode())
                .currentStatus(status.getCurrentStatus())
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .delayedAt(status.getDelayedAt())
                .updatedBy(status.getUpdatedBy())
                .notes(status.getNotes())
                .updatedAt(status.getUpdatedAt())
                .build();
    }

    private TrackingUpdateResponse toTrackingUpdateResponse(TrackingUpdate update, TransportTaskRef task) {
        return TrackingUpdateResponse.builder()
                .transportTaskCode(task.transportTaskCode())
                .updateType(update.getUpdateType())
                .previousStatus(update.getPreviousStatus())
                .newStatus(update.getNewStatus())
                .delayed(update.isDelayed())
                .delayReason(update.getDelayReason())
                .latitude(update.getLatitude())
                .longitude(update.getLongitude())
                .updatedBy(update.getUpdatedBy())
                .notes(update.getNotes())
                .recordedAt(update.getRecordedAt())
                .build();
    }
}