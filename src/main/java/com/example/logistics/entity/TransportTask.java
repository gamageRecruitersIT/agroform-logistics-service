package com.example.logistics.entity;

import com.example.logistics.entity.enums.TransportTaskStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents the operational execution of a TransportRequest.
 * Created by a transporter when assigning a vehicle and driver to an accepted request.
 *
 * transporter_id and driver_id are plain UUID cross-service references to Identity Service.
 * vehicle_id has a FK to the local vehicle table (within Logistics Service).
 * transport_request_id has a FK and a UNIQUE constraint — one task per request.
 */
@Entity
@Table(name = "transport_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transport_task_id", updatable = false, nullable = false)
    private UUID transportTaskId;

    @Column(name = "transport_task_code", nullable = false, unique = true, updatable = false, length = 20)
    private String transportTaskCode;

    // FK to TransportRequest within Logistics Service
    @Column(name = "transport_request_id", nullable = false, unique = true)
    private UUID transportRequestId;

    // Cross-service reference — Identity Service (transporter profile)
    @Column(name = "transporter_id", nullable = false)
    private UUID transporterId;

    // FK to Vehicle — within Logistics Service
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    // Cross-service reference — Identity Service (driver profile)
    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;

    @Column(name = "estimated_cost", precision = 14, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost", precision = 14, scale = 2)
    private BigDecimal actualCost;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "task_status", nullable = false)
    @Builder.Default
    private TransportTaskStatus taskStatus = TransportTaskStatus.ASSIGNED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
