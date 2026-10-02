package com.example.logistics.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transport_cost_rule", schema = "public")
@Data
public class TransportCostRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "cost_rule_id", updatable = false, nullable = false)
    private UUID costRuleId;

    @Column(name = "transporter_id", nullable = false)
    private UUID transporterId;

    @Column(name = "cost_per_km", nullable = false)
    private BigDecimal costPerKm;

    @Column(name = "cost_per_kg", nullable = false)
    private BigDecimal costPerKg;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}