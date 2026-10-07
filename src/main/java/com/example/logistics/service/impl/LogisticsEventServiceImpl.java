package com.example.logistics.service.impl;

import com.example.logistics.dto.event.LogisticsKafkaEvent;
import com.example.logistics.dto.response.LogisticsEventResponse;
import com.example.logistics.entity.LogisticsEvent;
import com.example.logistics.enums.LogisticsEventType;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.LogisticsEventRepository;
import com.example.logistics.service.LogisticsEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
//@ConditionalOnBean(LogisticsEventRepository.class)
public class LogisticsEventServiceImpl implements LogisticsEventService {

    private final LogisticsEventRepository eventRepository;
    private final LogisticsEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void recordEvent(
            String transportRequestCode,
            String taskCode,
            String vehicleCode,
            LogisticsEventType eventType,
            String description,
            String createdBy
    ) {
        if (transportRequestCode == null || transportRequestCode.isBlank()) {
            throw new BadRequestException("transportRequestCode is required");
        }
        if (eventType == null) {
            throw new BadRequestException("eventType is required");
        }

        LogisticsEvent event = LogisticsEvent.builder()
            .transportRequestId(findId("transport_request", "transport_request_code", transportRequestCode))
            .transportTaskId(findId("transport_task", "transport_task_code", taskCode))
            .vehicleId(findId("vehicle", "vehicle_code", vehicleCode))
            .eventType(eventType)
            .description(description)
            .createdBy(toUuid(createdBy))
            .build();

        LogisticsEvent saved = eventRepository.save(event);
        LogisticsKafkaEvent kafkaEvent = new LogisticsKafkaEvent(
                saved.getEventId().toString(),
                eventType.name(),
                transportRequestCode,
                taskCode,
                vehicleCode,
                description,
                createdBy,
                saved.getCreatedAt()
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    eventPublisher.publish(kafkaEvent);
                }
            });
        } else {
            eventPublisher.publish(kafkaEvent);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LogisticsEventResponse> getTransportRequestHistory(String transportRequestCode) {
        if (!transportRequestExists(transportRequestCode)) {
            throw new ResourceNotFoundException(
                    "Transport request not found: " + transportRequestCode
            );
        }

        return eventRepository.findByTransportRequestCode(transportRequestCode).stream()
                .map(event -> new LogisticsEventResponse(
                        event.getEventId().toString(),
                        event.getEventType().name(),
                        event.getDescription(),
                        event.getCreatedAt().toString()
                ))
                .toList();
    }

    private boolean transportRequestExists(String transportRequestCode) {
        if (transportRequestCode == null || transportRequestCode.isBlank()) {
            return false;
        }

        Long requestCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transport_request WHERE transport_request_code = ?",
            Long.class,
                transportRequestCode
        );
        return requestCount != null && requestCount > 0;
    }

    private UUID findId(String table, String codeColumn, String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        UUID id = jdbcTemplate.query(
                "SELECT " + table + "_id FROM " + table + " WHERE " + codeColumn + " = ?",
                resultSet -> resultSet.next() ? resultSet.getObject(1, UUID.class) : null,
                code
        );
        if (id == null) {
            throw new ResourceNotFoundException(table + " not found: " + code);
        }
        return id;
    }

    private UUID toUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("createdBy must be a valid UUID", ex);
        }
    }
}