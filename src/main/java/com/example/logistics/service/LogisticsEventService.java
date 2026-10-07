package com.example.logistics.service;

import com.example.logistics.dto.response.LogisticsEventResponse;
import com.example.logistics.enums.LogisticsEventType;

import java.util.List;

public interface LogisticsEventService {

    void recordEvent(
            String transportRequestCode,
            String taskCode,
            String vehicleCode,
            LogisticsEventType eventType,
            String description,
            String createdBy
    );

    List<LogisticsEventResponse> getTransportRequestHistory(
            String transportRequestCode
    );
}