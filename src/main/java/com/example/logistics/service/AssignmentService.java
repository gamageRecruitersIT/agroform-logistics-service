package com.example.logistics.service;

import com.example.logistics.dto.request.AssignmentRequestDto;
import com.example.logistics.dto.response.AssignmentResponseDto;
import com.example.logistics.dto.response.TaskResponseDto;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    /**
     * Assigns a driver and vehicle to an accepted transport request.
     * Atomically creates TransportTask, DriverAssignment, and initialises DeliveryStatus.
     * Throws VehicleNotAvailableException if vehicle is not AVAILABLE.
     * Throws DriverAlreadyAssignedException if driver already has an active assignment.
     * Throws ResourceNotFoundException if the transport request code does not exist.
     * Throws BadRequestException if the transport request is not in ACCEPTED status.
     *
     * @param transporterId UUID of the transporter (extracted from JWT).
     * @param dto           Assignment request payload.
     * @return AssignmentResponseDto
     */
    AssignmentResponseDto assignDriverAndVehicle(UUID transporterId, AssignmentRequestDto dto);

    /**
     * Gets the current active assignment for a driver.
     *
     * @param driverId UUID of the driver.
     * @return AssignmentResponseDto or throws ResourceNotFoundException.
     */
    AssignmentResponseDto getCurrentAssignment(UUID driverId);

    /**
     * Gets the full assignment history for a driver.
     *
     * @param driverId UUID of the driver.
     * @return List of AssignmentResponseDto, newest first.
     */
    List<AssignmentResponseDto> getDriverAssignmentHistory(UUID driverId);

    /**
     * Gets the transport task linked to a transport request code.
     *
     * @param transportRequestCode The public request code.
     * @return TaskResponseDto or throws ResourceNotFoundException.
     */
    TaskResponseDto getTaskByRequestCode(String transportRequestCode);

    /**
     * Gets all tasks for a transporter.
     *
     * @param transporterId UUID of the transporter.
     * @return List of TaskResponseDto, newest first.
     */
    List<TaskResponseDto> getTasksByTransporter(UUID transporterId);

    /**
     * Gets all tasks for a driver (task history).
     *
     * @param driverId UUID of the driver.
     * @return List of TaskResponseDto, newest first.
     */
    List<TaskResponseDto> getTasksByDriver(UUID driverId);
}
