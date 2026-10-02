package com.example.logistics.service.impl;

import com.example.logistics.dto.request.AssignmentRequestDto;
import com.example.logistics.dto.response.AssignmentResponseDto;
import com.example.logistics.dto.response.TaskResponseDto;
import com.example.logistics.entity.DriverAssignment;
import com.example.logistics.entity.TransportRequest;
import com.example.logistics.entity.TransportTask;
import com.example.logistics.entity.Vehicle;
import com.example.logistics.entity.enums.TransportRequestStatusEnum;
import com.example.logistics.entity.enums.TransportTaskStatus;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.DriverAlreadyAssignedException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.DriverAssignmentRepository;
import com.example.logistics.repository.TransportRequestRepository;
import com.example.logistics.repository.TransportTaskRepository;
import com.example.logistics.repository.VehicleRepository;
import com.example.logistics.service.AssignmentService;
import com.example.logistics.service.DeliveryTrackingService;
import com.example.logistics.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssignmentServiceImpl implements AssignmentService {

    private final TransportTaskRepository transportTaskRepository;
    private final DriverAssignmentRepository driverAssignmentRepository;
    private final TransportRequestRepository transportRequestRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;           // Vasitha's service
    private final DeliveryTrackingService deliveryTrackingService; // Dilum's service

    @Override
    @Transactional
    public AssignmentResponseDto assignDriverAndVehicle(UUID transporterId, AssignmentRequestDto dto) {
        log.info("Assigning driver {} and vehicle {} by transporter {}",
                dto.getDriverId(), dto.getVehicleId(), transporterId);

        // Step 1: Validate transport request exists and is ACCEPTED
        TransportRequest request = transportRequestRepository
                .findByTransportRequestCode(dto.getTransportRequestCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transport request not found: " + dto.getTransportRequestCode()));

        if (request.getRequestStatus() != TransportRequestStatusEnum.ACCEPTED) {
            throw new BadRequestException(
                    "Transport request must be in ACCEPTED status to assign. Current status: "
                    + request.getRequestStatus());
        }

        // Step 2: Check no task already exists for this request
        if (transportTaskRepository.existsByTransportRequestId(request.getTransportRequestId())) {
            throw new BadRequestException(
                    "A task already exists for transport request: " + dto.getTransportRequestCode());
        }

        // Step 3: Check driver is free (no active assignment)
        // DB also enforces this via partial unique index uq_driver_active_assignment,
        // but we check here first for a clean error message.
        if (driverAssignmentRepository.existsByDriverIdAndIsActiveTrue(dto.getDriverId())) {
            throw new DriverAlreadyAssignedException(
                    "Driver " + dto.getDriverId() + " is already assigned to an active task.");
        }

        // Step 4+5: Validate vehicle is AVAILABLE and reserve it atomically (row-locked)
        // markVehicleAssigned validates, throws VehicleNotAvailableException if not AVAILABLE,
        // then sets ASSIGNED — Vasitha's service rejects setting ASSIGNED via updateVehicleAvailability directly.
        Vehicle vehicle = vehicleRepository.findById(dto.getVehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + dto.getVehicleId()));
        vehicleService.markVehicleAssigned(vehicle.getVehicleCode());

        // Step 6: Generate task code
        String taskCode = "TTK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Step 7: Create TransportTask
        TransportTask task = TransportTask.builder()
                .transportTaskCode(taskCode)
                .transportRequestId(request.getTransportRequestId())
                .transporterId(transporterId)
                .vehicleId(dto.getVehicleId())
                .driverId(dto.getDriverId())
                .assignedAt(OffsetDateTime.now())
                .taskStatus(TransportTaskStatus.ASSIGNED)
                .build();

        TransportTask savedTask = transportTaskRepository.saveAndFlush(task); // flush before Dilum's initializeTracking FK lookup

        // Step 8: Create DriverAssignment record
        DriverAssignment assignment = DriverAssignment.builder()
                .driverId(dto.getDriverId())
                .transporterId(transporterId)
                .vehicleId(dto.getVehicleId())
                .transportTaskId(savedTask.getTransportTaskId())
                .assignedAt(OffsetDateTime.now())
                .isActive(true)
                .build();

        DriverAssignment savedAssignment = driverAssignmentRepository.save(assignment);

        // Step 9: Initialize DeliveryStatus (AWAITING_PICKUP) — Dilum's service
        deliveryTrackingService.initializeTracking(savedTask.getTransportTaskId());

        // Step 10: TODO (Gayani) — Publish DRIVER_ASSIGNED + VEHICLE_ASSIGNED Kafka events

        log.info("Assignment complete. Task: {}, Assignment: {}",
                savedTask.getTransportTaskCode(), savedAssignment.getAssignmentId());

        return mapToAssignmentResponse(savedAssignment, savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentResponseDto getCurrentAssignment(UUID driverId) {
        DriverAssignment assignment = driverAssignmentRepository
                .findByDriverIdAndIsActiveTrue(driverId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active assignment found for driver: " + driverId));

        TransportTask task = transportTaskRepository
                .findById(assignment.getTransportTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Task not found for assignment."));

        return mapToAssignmentResponse(assignment, task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentResponseDto> getDriverAssignmentHistory(UUID driverId) {
        return driverAssignmentRepository.findByDriverIdOrderByAssignedAtDesc(driverId)
                .stream()
                .map(a -> {
                    TransportTask task = transportTaskRepository
                            .findById(a.getTransportTaskId())
                            .orElse(null);
                    return mapToAssignmentResponse(a, task);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponseDto getTaskByRequestCode(String transportRequestCode) {
        TransportRequest request = transportRequestRepository
                .findByTransportRequestCode(transportRequestCode)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transport request not found: " + transportRequestCode));

        TransportTask task = transportTaskRepository
                .findByTransportRequestId(request.getTransportRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No task found for transport request: " + transportRequestCode));

        return mapToTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByTransporter(UUID transporterId) {
        return transportTaskRepository
                .findByTransporterIdOrderByAssignedAtDesc(transporterId)
                .stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponseDto> getTasksByDriver(UUID driverId) {
        return transportTaskRepository
                .findByDriverIdOrderByAssignedAtDesc(driverId)
                .stream()
                .map(this::mapToTaskResponse)
                .collect(Collectors.toList());
    }

    // ─── Mapping Helpers ────────────────────────────────────────────────────────

    private AssignmentResponseDto mapToAssignmentResponse(DriverAssignment assignment, TransportTask task) {
        return AssignmentResponseDto.builder()
                .assignmentId(assignment.getAssignmentId())
                .transportTaskCode(task != null ? task.getTransportTaskCode() : null)
                .transportRequestId(task != null ? task.getTransportRequestId() : null)
                .driverId(assignment.getDriverId())
                .vehicleId(assignment.getVehicleId())
                .transporterId(assignment.getTransporterId())
                .assignedAt(assignment.getAssignedAt())
                .isActive(assignment.getIsActive())
                .build();
    }

    private TaskResponseDto mapToTaskResponse(TransportTask task) {
        return TaskResponseDto.builder()
                .transportTaskId(task.getTransportTaskId())
                .transportTaskCode(task.getTransportTaskCode())
                .transportRequestId(task.getTransportRequestId())
                .transporterId(task.getTransporterId())
                .vehicleId(task.getVehicleId())
                .driverId(task.getDriverId())
                .assignedAt(task.getAssignedAt())
                .acceptedAt(task.getAcceptedAt())
                .estimatedCost(task.getEstimatedCost())
                .actualCost(task.getActualCost())
                .taskStatus(task.getTaskStatus())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
