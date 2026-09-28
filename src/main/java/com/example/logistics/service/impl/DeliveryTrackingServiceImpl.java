package com.example.logistics.service.impl;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;
import com.example.logistics.entity.DeliveryStatus;
import com.example.logistics.entity.DeliveryStatusEnum;
import com.example.logistics.entity.TrackingUpdate;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.DuplicateResourceException;
import com.example.logistics.exception.InvalidDeliveryStatusException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.DeliveryStatusRepository;
import com.example.logistics.repository.TrackingUpdateRepository;
import com.example.logistics.service.DeliveryTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryTrackingServiceImpl implements DeliveryTrackingService {

    private final DeliveryStatusRepository deliveryStatusRepository;
    private final TrackingUpdateRepository trackingUpdateRepository;

    @Override
    @Transactional
    public DeliveryStatusResponse initializeTracking(UUID transportTaskId) {
        if (transportTaskId == null) {
            throw new BadRequestException("transportTaskId is required");
        }
        if (deliveryStatusRepository.existsByTransportTaskId(transportTaskId)) {
            throw new DuplicateResourceException(
                    "Delivery tracking already initialized for transport task " + transportTaskId);
        }

        DeliveryStatus status = DeliveryStatus.builder()
                .transportTaskId(transportTaskId)
                .currentStatus(DeliveryStatusEnum.AWAITING_PICKUP)
                .delayed(false)
                .build();
        status = deliveryStatusRepository.save(status);

        TrackingUpdate initialEvent = TrackingUpdate.builder()
                .transportTaskId(transportTaskId)
                .previousStatus(null)
                .newStatus(DeliveryStatusEnum.AWAITING_PICKUP)
                .delayed(false)
                .notes("Tracking initialized on transport task creation")
                .build();
        trackingUpdateRepository.save(initialEvent);

        log.info("Initialized delivery tracking for transport task {}", transportTaskId);
        return toDeliveryStatusResponse(status);
    }

    @Override
    public DeliveryStatusResponse getCurrentStatus(UUID transportTaskId) {
        return toDeliveryStatusResponse(findStatusOrThrow(transportTaskId));
    }

    @Override
    public List<TrackingUpdateResponse> getHistory(UUID transportTaskId) {
        if (!deliveryStatusRepository.existsByTransportTaskId(transportTaskId)) {
            throw new ResourceNotFoundException(
                    "No delivery tracking found for transport task " + transportTaskId);
        }
        return trackingUpdateRepository.findByTransportTaskIdOrderByRecordedAtAsc(transportTaskId)
                .stream()
                .map(this::toTrackingUpdateResponse)
                .toList();
    }

    @Override
    @Transactional
    public DeliveryStatusResponse updateStatus(UUID transportTaskId, TrackingStatusUpdateRequest request) {
        DeliveryStatus status = findStatusOrThrow(transportTaskId);
        DeliveryStatusEnum previous = status.getCurrentStatus();
        DeliveryStatusEnum next = request.getNewStatus();

        if (previous == next) {
            throw new BadRequestException(
                    "Transport task " + transportTaskId + " is already in status " + previous);
        }

        if (!request.isAuthorizedOverride()) {
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
        status.setUpdatedBy(request.getUpdatedBy());
        if (request.getNotes() != null) {
            status.setNotes(request.getNotes());
        }
        status = deliveryStatusRepository.save(status);

        TrackingUpdate event = TrackingUpdate.builder()
                .transportTaskId(transportTaskId)
                .previousStatus(previous)
                .newStatus(next)
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .updatedBy(request.getUpdatedBy())
                .notes(request.getNotes())
                .build();
        trackingUpdateRepository.save(event);

        log.info("Transport task {} delivery status: {} -> {}", transportTaskId, previous, next);
        return toDeliveryStatusResponse(status);
    }

    @Override
    @Transactional
    public DeliveryStatusResponse flagDelay(UUID transportTaskId, DelayFlagRequest request) {
        DeliveryStatus status = findStatusOrThrow(transportTaskId);

        if (status.getCurrentStatus().isTerminal()) {
            throw new InvalidDeliveryStatusException(
                    "Cannot flag a delay on transport task " + transportTaskId
                            + "; delivery already reached " + status.getCurrentStatus());
        }

        status.setDelayed(true);
        status.setDelayReason(request.getReason());
        status.setDelayedAt(OffsetDateTime.now());
        status.setUpdatedBy(request.getUpdatedBy());
        status = deliveryStatusRepository.save(status);

        TrackingUpdate event = TrackingUpdate.builder()
                .transportTaskId(transportTaskId)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(true)
                .delayReason(request.getReason())
                .updatedBy(request.getUpdatedBy())
                .notes("Delay flagged: " + request.getReason())
                .build();
        trackingUpdateRepository.save(event);

        log.warn("Transport task {} flagged DELAYED: {}", transportTaskId, request.getReason());
        return toDeliveryStatusResponse(status);
    }

    @Override
    @Transactional
    public DeliveryStatusResponse resolveDelay(UUID transportTaskId, DelayFlagRequest request) {
        DeliveryStatus status = findStatusOrThrow(transportTaskId);

        if (!status.isDelayed()) {
            throw new BadRequestException(
                    "Transport task " + transportTaskId + " is not currently flagged as delayed");
        }

        status.setDelayed(false);
        status.setDelayReason(null);
        status.setDelayedAt(null);
        status.setUpdatedBy(request.getUpdatedBy());
        status = deliveryStatusRepository.save(status);

        TrackingUpdate event = TrackingUpdate.builder()
                .transportTaskId(transportTaskId)
                .previousStatus(status.getCurrentStatus())
                .newStatus(status.getCurrentStatus())
                .delayed(false)
                .updatedBy(request.getUpdatedBy())
                .notes("Delay resolved: " + request.getReason())
                .build();
        trackingUpdateRepository.save(event);

        log.info("Transport task {} delay resolved", transportTaskId);
        return toDeliveryStatusResponse(status);
    }

    private DeliveryStatus findStatusOrThrow(UUID transportTaskId) {
        return deliveryStatusRepository.findByTransportTaskId(transportTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No delivery tracking found for transport task " + transportTaskId));
    }

    private DeliveryStatusResponse toDeliveryStatusResponse(DeliveryStatus status) {
        return DeliveryStatusResponse.builder()
                .transportTaskId(status.getTransportTaskId())
                .currentStatus(status.getCurrentStatus())
                .delayed(status.isDelayed())
                .delayReason(status.getDelayReason())
                .delayedAt(status.getDelayedAt())
                .updatedBy(status.getUpdatedBy())
                .notes(status.getNotes())
                .updatedAt(status.getUpdatedAt())
                .build();
    }

    private TrackingUpdateResponse toTrackingUpdateResponse(TrackingUpdate update) {
        return TrackingUpdateResponse.builder()
                .trackingUpdateId(update.getTrackingUpdateId())
                .transportTaskId(update.getTransportTaskId())
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
