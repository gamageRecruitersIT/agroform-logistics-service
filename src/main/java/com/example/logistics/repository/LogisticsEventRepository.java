package com.example.logistics.repository;

import com.example.logistics.entity.LogisticsEvent;
import com.example.logistics.enums.LogisticsEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LogisticsEventRepository
        extends JpaRepository<LogisticsEvent, UUID> {

    List<LogisticsEvent> findByTransportRequestIdOrderByCreatedAtAsc(
            UUID transportRequestId
    );

    List<LogisticsEvent> findByTransportTaskIdOrderByCreatedAtAsc(
            UUID transportTaskId
    );

    List<LogisticsEvent> findByVehicleIdOrderByCreatedAtAsc(
            UUID vehicleId
    );

    long countByEventType(LogisticsEventType eventType);

    long countByEventTypeAndCreatedAtBetween(
            LogisticsEventType eventType,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Query("""
            SELECT e.eventType, COUNT(e)
            FROM LogisticsEvent e
            GROUP BY e.eventType
            ORDER BY e.eventType
            """)
    List<Object[]> countEventsByType();

    /*
     * Public transport request code -> internal UUID
     * -> event history
     */
    @Query(value = """
            SELECT le.*
            FROM logistics_event le
            INNER JOIN transport_request tr
                ON tr.transport_request_id = le.transport_request_id
            WHERE tr.transport_request_code = :transportRequestCode
            ORDER BY le.created_at ASC
            """, nativeQuery = true)
    List<LogisticsEvent> findByTransportRequestCode(
            @Param("transportRequestCode") String transportRequestCode
    );
}