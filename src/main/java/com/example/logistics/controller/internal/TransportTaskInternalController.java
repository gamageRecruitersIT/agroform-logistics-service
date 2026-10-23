package com.example.logistics.controller.internal;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.TransportTaskDetailResponse;
import com.example.logistics.service.TransportTaskDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal endpoint for Warehouse & Inventory Service.
 * Called back after Warehouse receives a delivery notification, to fetch the task snapshot.
 * Internal service-to-service only (do not route it through the gateway).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logistics/internal/tasks")
public class TransportTaskInternalController {

    private final TransportTaskDetailService transportTaskDetailService;

    @GetMapping("/{transportTaskId}")
    public ResponseEntity<ApiResponse<TransportTaskDetailResponse>> getTaskDetail(
            @PathVariable UUID transportTaskId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Transport task details retrieved successfully.",
                transportTaskDetailService.getTaskDetail(transportTaskId)));
    }
}
