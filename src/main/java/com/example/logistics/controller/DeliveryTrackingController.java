package com.example.logistics.controller;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;
import com.example.logistics.service.DeliveryTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Delivery Status & Tracking APIs.
 * Base path: /api/v1/logistics/tracking
 * Owner: Dilum.
 *
 * Notes for the rest of the team:
 * - initializeTracking is meant to be called from Chamuditha's assignment flow
 *   right after a transport_task row is created.
 * - Every status change and every delay flag/resolve is logged as an immutable
 *   tracking_update row, so Gayani's LogisticsEvent/Kafka publishing and Navodya's
 *   notification Feign client can react to it.
 * - Role checks (DRIVER may submit, TRANSPORTER may override) are enforced by
 *   Navodya's authorization module upstream of these endpoints.
 */
@RestController
@RequestMapping("/api/v1/logistics/tracking")
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryTrackingService deliveryTrackingService;

    @PostMapping("/{transportTaskId}/init")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> initializeTracking(
            @PathVariable UUID transportTaskId) {
        DeliveryStatusResponse response = deliveryTrackingService.initializeTracking(transportTaskId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery tracking initialized", response));
    }

    @GetMapping("/{transportTaskId}")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> getCurrentStatus(
            @PathVariable UUID transportTaskId) {
        DeliveryStatusResponse response = deliveryTrackingService.getCurrentStatus(transportTaskId);
        return ResponseEntity.ok(ApiResponse.success("Current delivery status retrieved", response));
    }

    @GetMapping("/{transportTaskId}/history")
    public ResponseEntity<ApiResponse<List<TrackingUpdateResponse>>> getHistory(
            @PathVariable UUID transportTaskId) {
        List<TrackingUpdateResponse> response = deliveryTrackingService.getHistory(transportTaskId);
        return ResponseEntity.ok(ApiResponse.success("Tracking history retrieved", response));
    }

    @PatchMapping("/{transportTaskId}/status")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> updateStatus(
            @PathVariable UUID transportTaskId,
            @Valid @RequestBody TrackingStatusUpdateRequest request) {
        DeliveryStatusResponse response = deliveryTrackingService.updateStatus(transportTaskId, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated", response));
    }

    @PostMapping("/{transportTaskId}/delay")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> flagDelay(
            @PathVariable UUID transportTaskId,
            @Valid @RequestBody DelayFlagRequest request) {
        DeliveryStatusResponse response = deliveryTrackingService.flagDelay(transportTaskId, request);
        return ResponseEntity.ok(ApiResponse.success("Delivery flagged as delayed", response));
    }

    @PostMapping("/{transportTaskId}/delay/resolve")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> resolveDelay(
            @PathVariable UUID transportTaskId,
            @Valid @RequestBody DelayFlagRequest request) {
        DeliveryStatusResponse response = deliveryTrackingService.resolveDelay(transportTaskId, request);
        return ResponseEntity.ok(ApiResponse.success("Delay resolved", response));
    }
}
