package com.example.logistics.service.impl;

import com.example.logistics.dto.event.LogisticsKafkaEvent;

public interface LogisticsEventPublisher {

    void publish(LogisticsKafkaEvent event);
}