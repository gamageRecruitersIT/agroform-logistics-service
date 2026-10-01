package com.example.logistics.entity;


import com.example.logistics.entity.enums.TransportRequestStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transport_request")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transport_request_id", updatable = false, nullable = false)
    private UUID transportRequestId;

    @Column(name = "transport_request_code", nullable = false, unique = true, updatable = false)
    private String transportRequestCode;

    @Column(name = "farmer_id", nullable = false)
    private UUID farmerId;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @Column(name = "auction_ref_code", nullable = false)
    private String auctionRefCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", nullable = false)
    @Builder.Default
    private String quantityUnit = "KG";

    @Column(name = "pickup_latitude", precision = 10, scale = 7)
    private BigDecimal pickupLatitude;

    @Column(name = "pickup_longitude", precision = 10, scale = 7)
    private BigDecimal pickupLongitude;

    @Column(name = "pickup_address", columnDefinition = "TEXT")
    private String pickupAddress;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "warehouse_latitude", precision = 10, scale = 7)
    private BigDecimal warehouseLatitude;

    @Column(name = "warehouse_longitude", precision = 10, scale = 7)
    private BigDecimal warehouseLongitude;

    @Column(name = "estimated_distance_km", precision = 10, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "request_status", nullable = false)
    @Builder.Default
    private TransportRequestStatusEnum requestStatus = TransportRequestStatusEnum.PENDING;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}