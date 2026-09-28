package com.example.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable, append-only audit trail of every delivery status transition
 * (and every delay flag/resolve) for a transport task.
 * Never updated or deleted — this is the source of truth for tracking history
 * shown to farmer, transporter, and driver.
 *
 * Owner: Dilum — Delivery Status & Tracking.
 */
@Entity
@Table(name = "tracking_update")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingUpdate {

    @Id
    @GeneratedValue
    @Column(name = "tracking_update_id", nullable = false, updatable = false)
    private UUID trackingUpdateId;

    @Column(name = "transport_task_id", nullable = false)
    private UUID transportTaskId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "previous_status", columnDefinition = "delivery_status_enum")
    private DeliveryStatusEnum previousStatus;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "new_status", nullable = false, columnDefinition = "delivery_status_enum")
    private DeliveryStatusEnum newStatus;

    /**
     * Snapshot of the delay flag at the moment this row was recorded.
     * A row can represent a pure delay flag/resolve event when previousStatus == newStatus.
     */
    @Column(name = "is_delayed", nullable = false)
    private boolean delayed;

    @Column(name = "delay_reason")
    private String delayReason;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = OffsetDateTime.now();
        }
    }
}