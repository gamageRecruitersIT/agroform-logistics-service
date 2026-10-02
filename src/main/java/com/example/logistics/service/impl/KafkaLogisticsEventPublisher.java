package com.example.logistics.service.impl;

import com.example.logistics.dto.event.LogisticsKafkaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaLogisticsEventPublisher implements LogisticsEventPublisher {

    private final KafkaTemplate<String, LogisticsKafkaEvent> kafkaTemplate;

    @Value("${logistics.kafka.topic:logistics-events}")
    private String topic;

    @Value("${kafka.enabled:true}")
    private boolean enabled;

    @Override
    public void publish(LogisticsKafkaEvent event) {
        if (enabled) {
            kafkaTemplate.send(topic, event.eventId(), event);
        }
    }
}