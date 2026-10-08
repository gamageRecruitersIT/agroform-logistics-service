package com.example.logistics.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.logistics.dto.request.DelayFlagRequest;
import com.example.logistics.dto.request.LocationUpdateRequest;
import com.example.logistics.dto.request.TrackingStatusUpdateRequest;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.DeliveryStatusResponse;
import com.example.logistics.dto.response.TrackingUpdateResponse;
import com.example.logistics.security.CurrentUser;
import com.example.logistics.service.DeliveryTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Delivery Status & Tracking APIs.
 * Base path: /api/v1/logistics/tracking
 * Owner: Dilum.
 *
 * Tasks are addressed by their public code (transport_task_code, e.g. TTK-AB12CD34);
 * internal UUIDs are never exposed. The caller (id + role) comes from the gateway-forwarded
 * identity, see CurrentUserArgumentResolver. All authorization decisions live in the service layer.
 *
 * Roles:
 *   DRIVER      - submit status updates / live location / delay flag (own tasks)
 *   TRANSPORTER - initialize tracking, override status, flag/resolve delay (own tasks)
 *   FARMER      - read-only (own tasks)
 */
@RestController
@PreAuthorize("hasAnyRole('FARMER','TRANSPORTER','DRIVER')")
@RequestMapping("/api/v1/logistics/tracking")
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryTrackingService deliveryTrackingService;

    @PostMapping("/{taskCode}/init")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> initializeTracking(
            @PathVariable String taskCode, CurrentUser user) {
        DeliveryStatusResponse response = deliveryTrackingService.initializeTracking(taskCode, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery tracking initialized", response));
    }

    @GetMapping("/my-tasks")
    public ResponseEntity<ApiResponse<List<DeliveryStatusResponse>>> getMyTasks(CurrentUser user) {
        List<DeliveryStatusResponse> response = deliveryTrackingService.getMyTasks(user);
        return ResponseEntity.ok(ApiResponse.success("Delivery statuses retrieved", response));
    }

    @GetMapping("/{taskCode}")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> getCurrentStatus(
            @PathVariable String taskCode, CurrentUser user) {
        DeliveryStatusResponse response = deliveryTrackingService.getCurrentStatus(taskCode, user);
        return ResponseEntity.ok(ApiResponse.success("Current delivery status retrieved", response));
    }

    @GetMapping("/{taskCode}/history")
    public ResponseEntity<ApiResponse<List<TrackingUpdateResponse>>> getHistory(
            @PathVariable String taskCode,
            @RequestParam(defaultValue = "false") boolean includeLocations,
            CurrentUser user) {
        List<TrackingUpdateResponse> response =
                deliveryTrackingService.getHistory(taskCode, includeLocations, user);
        return ResponseEntity.ok(ApiResponse.success("Tracking history retrieved", response));
    }

    @GetMapping("/{taskCode}/location/latest")
    public ResponseEntity<ApiResponse<TrackingUpdateResponse>> getLatestLocation(
            @PathVariable String taskCode, CurrentUser user) {
        TrackingUpdateResponse response = deliveryTrackingService.getLatestLocation(taskCode, user);
        return ResponseEntity.ok(ApiResponse.success("Latest location retrieved", response));
    }

    @PostMapping("/{taskCode}/location")
    public ResponseEntity<ApiResponse<TrackingUpdateResponse>> submitLocation(
            @PathVariable String taskCode,
            @Valid @RequestBody LocationUpdateRequest request,
            CurrentUser user) {
        TrackingUpdateResponse response = deliveryTrackingService.submitLocation(taskCode, request, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Location recorded", response));
    }

    @PatchMapping("/{taskCode}/status")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> updateStatus(
            @PathVariable String taskCode,
            @Valid @RequestBody TrackingStatusUpdateRequest request,
            CurrentUser user) {
        DeliveryStatusResponse response = deliveryTrackingService.updateStatus(taskCode, request, user);
        return ResponseEntity.ok(ApiResponse.success("Delivery status updated", response));
    }

    @PostMapping("/{taskCode}/delay")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> flagDelay(
            @PathVariable String taskCode,
            @Valid @RequestBody DelayFlagRequest request,
            CurrentUser user) {
        DeliveryStatusResponse response = deliveryTrackingService.flagDelay(taskCode, request, user);
        return ResponseEntity.ok(ApiResponse.success("Delivery flagged as delayed", response));
    }

    @PostMapping("/{taskCode}/delay/resolve")
    public ResponseEntity<ApiResponse<DeliveryStatusResponse>> resolveDelay(
            @PathVariable String taskCode,
            @Valid @RequestBody DelayFlagRequest request,
            CurrentUser user) {
        DeliveryStatusResponse response = deliveryTrackingService.resolveDelay(taskCode, request, user);
        return ResponseEntity.ok(ApiResponse.success("Delay resolved", response));
    }
}
