package com.example.logistics.service;

import com.example.logistics.dto.response.TransportTaskDetailResponse;

import java.util.UUID;

public interface TransportTaskDetailService {

    /** Builds the task + request snapshot that Warehouse & Inventory Service needs on arrival. */
    TransportTaskDetailResponse getTaskDetail(UUID transportTaskId);
}
