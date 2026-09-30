package com.example.logistics.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicle_photo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiclePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "photo_id", updatable = false, nullable = false)
    private UUID photoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "photo_url", nullable = false, length = 500)
    private String photoUrl;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private OffsetDateTime uploadedAt;
}