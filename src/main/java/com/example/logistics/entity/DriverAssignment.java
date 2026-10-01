package com.example.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records every driver-to-vehicle and driver-to-task assignment.
 * Acts as an assignment history log.
 *
 * driver_id and transporter_id are plain UUID cross-service references to Identity Service.
 * One active assignment per driver enforced by partial unique index: uq_driver_active_assignment
 *   (driver_id WHERE is_active = TRUE) — defined in the Flyway migration.
 *
 * released_at is set when the driver completes or is removed from the active assignment.
 * No updated_at column — released_at serves as the targeted state-change timestamp.
 */
@Entity
@Table(name = "driver_assignment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    private UUID assignmentId;

    // Cross-service reference — Identity Service (driver profile)
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    // Cross-service reference — Identity Service (transporter profile)
    @Column(name = "transporter_id", nullable = false)
    private UUID transporterId;

    // FK to Vehicle within Logistics Service
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // FK to TransportTask within Logistics Service
    @Column(name = "transport_task_id", nullable = false)
    private UUID transportTaskId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    // Set when driver is released from this assignment
    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
