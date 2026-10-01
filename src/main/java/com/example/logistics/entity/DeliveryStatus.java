package com.example.logistics.entity;

import com.example.logistics.entity.enums.DeliveryStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Current delivery status for a transport task.
 * One row per transport_task_id (see uq_delivery_status_transport_task_id).
 * Full transition history lives in {@link TrackingUpdate}.
 *
 * Owner: Dilum — Delivery Status & Tracking.
 */
@Entity
@Table(name = "delivery_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatus {

    @Id
    @GeneratedValue
    @Column(name = "delivery_status_id", nullable = false, updatable = false)
    private UUID deliveryStatusId;

    @Column(name = "transport_task_id", nullable = false, unique = true)
    private UUID transportTaskId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "current_status", nullable = false, columnDefinition = "delivery_status_enum")
    private DeliveryStatusEnum currentStatus;

    /**
     * Delay flag — independent of currentStatus. Lets a task sit at, say, IN_TRANSIT
     * while flagged DELAYED, without inventing a backward/side status in the enum.
     * Enables downstream delay notifications (Navodya) and Kafka events (Gayani).
     */
    @Column(name = "is_delayed", nullable = false)
    private boolean delayed;

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "delayed_at")
    private OffsetDateTime delayedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.currentStatus == null) {
            this.currentStatus = DeliveryStatusEnum.AWAITING_PICKUP;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}