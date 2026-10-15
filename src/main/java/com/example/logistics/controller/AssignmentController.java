package com.example.logistics.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.logistics.dto.request.AssignmentRequestDto;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.AssignmentResponseDto;
import com.example.logistics.dto.response.TaskResponseDto;
import com.example.logistics.security.CurrentUser;
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
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ResponseEntity<ApiResponse<AssignmentResponseDto>> assignDriverAndVehicle(
            CurrentUser user,
            @Valid @RequestBody AssignmentRequestDto dto) {

        AssignmentResponseDto response = assignmentService.assignDriverAndVehicle(user.userId(), dto);
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
    @PreAuthorize("hasAnyRole('FARMER','TRANSPORTER','DRIVER')")
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
    @PreAuthorize("hasAnyRole('TRANSPORTER','DRIVER')")
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
    @PreAuthorize("hasAnyRole('FARMER','TRANSPORTER','DRIVER')")
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
    @PreAuthorize("hasRole('TRANSPORTER')")
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
    @PreAuthorize("hasAnyRole('TRANSPORTER','DRIVER')")
    public ResponseEntity<ApiResponse<List<TaskResponseDto>>> getTasksByDriver(
            @PathVariable UUID driverId) {

        List<TaskResponseDto> response = assignmentService.getTasksByDriver(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver task history retrieved.", response));
    }
}
