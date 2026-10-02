package com.example.logistics.repository;

import com.example.logistics.entity.TrackingUpdate;
import com.example.logistics.entity.enums.TrackingUpdateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackingUpdateRepository extends JpaRepository<TrackingUpdate, UUID> {

    List<TrackingUpdate> findByTransportTaskIdOrderByRecordedAtAsc(UUID transportTaskId);

    List<TrackingUpdate> findByTransportTaskIdOrderByRecordedAtDesc(UUID transportTaskId);

    /** History without the (frequent) live GPS pings. */
    List<TrackingUpdate> findByTransportTaskIdAndUpdateTypeNotOrderByRecordedAtAsc(
            UUID transportTaskId, TrackingUpdateType excludedType);

    /** Most recent row that carries GPS coordinates (location ping or status change). */
    Optional<TrackingUpdate> findFirstByTransportTaskIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByRecordedAtDesc(
            UUID transportTaskId);
}
