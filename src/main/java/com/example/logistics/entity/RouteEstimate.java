package com.example.logistics.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "route_estimate", schema = "public")
@Data
public class RouteEstimate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "route_estimate_id", updatable = false, nullable = false)
    private UUID routeEstimateId;

    @Column(name = "transport_request_id")
    private UUID transportRequestId;

    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "cost_per_km_used")
    private BigDecimal costPerKmUsed;

    @Column(name = "cost_per_kg_used")
    private BigDecimal costPerKgUsed;

    @Column(name = "weight_used")
    private Double weightUsed;

    @Column(name = "estimated_at")
    private LocalDateTime estimatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.estimatedAt == null) {
            this.estimatedAt = LocalDateTime.now();
        }
    }
}