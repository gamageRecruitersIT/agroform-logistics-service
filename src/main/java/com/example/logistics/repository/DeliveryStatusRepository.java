package com.example.logistics.repository;

import com.example.logistics.entity.DeliveryStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryStatusRepository extends JpaRepository<DeliveryStatus, UUID> {

    Optional<DeliveryStatus> findByTransportTaskId(UUID transportTaskId);

    boolean existsByTransportTaskId(UUID transportTaskId);

    List<DeliveryStatus> findByTransportTaskIdIn(Collection<UUID> transportTaskIds);

    /** Row lock so two simultaneous updates cannot both pass the transition check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DeliveryStatus d where d.transportTaskId = :transportTaskId")
    Optional<DeliveryStatus> findByTransportTaskIdForUpdate(@Param("transportTaskId") UUID transportTaskId);
}
