package com.example.logistics.repository;

import com.example.logistics.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryStatusRepository extends JpaRepository<DeliveryStatus, UUID> {

    Optional<DeliveryStatus> findByTransportTaskId(UUID transportTaskId);

    boolean existsByTransportTaskId(UUID transportTaskId);
}
