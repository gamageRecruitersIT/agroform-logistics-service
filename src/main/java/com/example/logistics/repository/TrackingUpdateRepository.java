package com.example.logistics.repository;

import com.example.logistics.entity.TrackingUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackingUpdateRepository extends JpaRepository<TrackingUpdate, UUID> {

    List<TrackingUpdate> findByTransportTaskIdOrderByRecordedAtAsc(UUID transportTaskId);

    List<TrackingUpdate> findByTransportTaskIdOrderByRecordedAtDesc(UUID transportTaskId);
}
