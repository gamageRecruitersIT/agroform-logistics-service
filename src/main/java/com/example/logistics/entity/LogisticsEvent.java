package com.example.logistics.entity;

import com.example.logistics.enums.LogisticsEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "logistics_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "transport_request_id")
    private UUID transportRequestId;

    @Column(name = "transport_task_id")
    private UUID transportTaskId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Convert(converter = LogisticsEventTypeConverter.class)
    @Column(
            name = "event_type",
            nullable = false,
            columnDefinition = "logistics_event_type_enum"
    )
    private LogisticsEventType eventType;

    @Column(name = "description")
    private String description;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

}