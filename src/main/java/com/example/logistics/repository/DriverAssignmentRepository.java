package com.example.logistics.repository;

import com.example.logistics.entity.DriverAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverAssignmentRepository extends JpaRepository<DriverAssignment, UUID> {

    /**
     * Check if a driver currently has an active assignment.
     * DB also enforces this via partial unique index: uq_driver_active_assignment.
     * This check is done in the service layer first for a clean error message.
     */
    boolean existsByDriverIdAndIsActiveTrue(UUID driverId);

    /**
     * Get the currently active assignment for a driver.
     */
    Optional<DriverAssignment> findByDriverIdAndIsActiveTrue(UUID driverId);

    /**
     * Get the full assignment history for a driver, ordered newest first.
     */
    List<DriverAssignment> findByDriverIdOrderByAssignedAtDesc(UUID driverId);

    /**
     * Get the active assignment for a specific task.
     */
    Optional<DriverAssignment> findByTransportTaskIdAndIsActiveTrue(UUID transportTaskId);
}
