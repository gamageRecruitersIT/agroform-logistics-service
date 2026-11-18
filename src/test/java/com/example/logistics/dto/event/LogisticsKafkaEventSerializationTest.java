package com.example.logistics.dto.event;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LogisticsKafkaEventSerializationTest {

    @Test
    void serializesCreatedAtAsIso8601() {
        LogisticsKafkaEvent event = new LogisticsKafkaEvent(
                "event-id",
                "TRANSPORT_REQUEST_CREATED",
                "TRQ-12345678",
                null,
                null,
                "Transport request created",
                "farmer-id",
                OffsetDateTime.parse("2026-11-18T16:44:07+05:30")
        );

        byte[] serialized = new JsonSerializer<LogisticsKafkaEvent>()
                .serialize("logistics-events", event);

        String json = new String(serialized, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"createdAt\":\"2026-11-18T16:44:07+05:30\""), json);
    }
}
