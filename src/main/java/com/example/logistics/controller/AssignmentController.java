package com.example.logistics.controller;

import com.example.logistics.dto.request.AssignmentRequestDto;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.AssignmentResponseDto;
import com.example.logistics.dto.response.TaskResponseDto;
import com.example.logistics.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Handles driver assignment and transport task management.
 * Base path: /api/v1/logistics/assignments
 *
 * Note: transporterId is currently passed as a header (X-Transporter-Id) as a
 * placeholder until Navodya's JWT extraction is integrated.
 */
@RestController
@RequestMapping("/api/v1/logistics/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * POST /api/v1/logistics/assignments
     * Assign a driver + vehicle to an accepted transport request.
     * Role: TRANSPORTER
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> assignDriverAndVehicle(
            // TODO (Navodya): Replace with @AuthenticationPrincipal or JWT claim extraction.
            @RequestHeader("X-Transporter-Id") UUID transporterId,
            @Valid @RequestBody AssignmentRequestDto dto) {

        AssignmentResponseDto response = assignmentService.assignDriverAndVehicle(transporterId, dto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver and vehicle assigned successfully.", response));
    }

    /**
     * GET /api/v1/logistics/assignments/driver/{driverId}/current
     * Get the current active assignment of a driver.
     * Role: TRANSPORTER, DRIVER, FARMER
     */
    @GetMapping("/driver/{driverId}/current")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> getCurrentAssignment(
            @PathVariable UUID driverId) {

        AssignmentResponseDto response = assignmentService.getCurrentAssignment(driverId);
        return ResponseEntity.ok(ApiResponse.success("Current assignment retrieved.", response));
    }

    /**
     * GET /api/v1/logistics/assignments/driver/{driverId}/history
     * Get the full assignment history for a driver.
     * Role: TRANSPORTER, DRIVER
     */
    @GetMapping("/driver/{driverId}/history")
    public ResponseEntity<ApiResponse<List<AssignmentResponseDto>>> getDriverHistory(
            @PathVariable UUID driverId) {

        List<AssignmentResponseDto> response = assignmentService.getDriverAssignmentHistory(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver assignment history retrieved.", response));
    }

    /**
     * GET /api/v1/logistics/assignments/tasks/request/{requestCode}
     * Get the transport task linked to a specific request code.
     * Role: TRANSPORTER, DRIVER, FARMER
     */
    @GetMapping("/tasks/request/{requestCode}")
    public ResponseEntity<ApiResponse<TaskResponseDto>> getTaskByRequestCode(
            @PathVariable String requestCode) {

        TaskResponseDto response = assignmentService.getTaskByRequestCode(requestCode);
        return ResponseEntity.ok(ApiResponse.success("Task retrieved.", response));
    }

    /**
     * GET /api/v1/logistics/assignments/tasks/transporter/{transporterId}
     * Get all tasks for a transporter.
     * Role: TRANSPORTER
     */
    @GetMapping("/tasks/transporter/{transporterId}")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getTasksByTransporter(
            @PathVariable UUID transporterId) {

        List<TaskResponseDto> response = assignmentService.getTasksByTransporter(transporterId);
        return ResponseEntity.ok(ApiResponse.success("Transporter tasks retrieved.", response));
    }

    /**
     * GET /api/v1/logistics/assignments/tasks/driver/{driverId}
     * Get all tasks (history) for a driver.
     * Role: DRIVER, TRANSPORTER
     */
    @GetMapping("/tasks/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getTasksByDriver(
            @PathVariable UUID driverId) {

        List<TaskResponseDto> response = assignmentService.getTasksByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver task history retrieved.", response));
    }
}
