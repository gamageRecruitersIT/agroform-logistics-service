package com.example.logistics.service.impl;

import com.example.logistics.enums.LogisticsEventType;
import com.example.logistics.service.LogisticsEventService;
import com.example.logistics.service.LogisticsWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
//@ConditionalOnBean(LogisticsEventService.class)
public class LogisticsWorkflowServiceImpl implements LogisticsWorkflowService {

    private final LogisticsEventService logisticsEventService;

    @Override
    public void onTransportRequestCreated(String transportRequestCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                null,
                null,
                LogisticsEventType.TRANSPORT_REQUEST_CREATED,
                "Transport request created",
                createdBy
        );
    }

    @Override
    public void onVehicleAssigned(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.VEHICLE_ASSIGNED,
                "Vehicle assigned to transport task",
                createdBy
        );
    }

    @Override
    public void onDriverAssigned(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.DRIVER_ASSIGNED,
                "Driver assigned to transport task",
                createdBy
        );
    }

    @Override
    public void onPickupStarted(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.PICKUP_STARTED,
                "Pickup started",
                createdBy
        );
    }

    @Override
    public void onInTransit(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.IN_TRANSIT,
                "Vehicle is in transit",
                createdBy
        );
    }

    @Override
    public void onDelivered(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.DELIVERED,
                "Goods delivered",
                createdBy
        );
    }

    @Override
    public void onUnloadedAtWarehouse(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.UNLOADED_AT_WAREHOUSE,
                "Goods unloaded at warehouse",
                createdBy
        );
    }

    @Override
    public void onTransportCancelled(String transportRequestCode, String taskCode, String vehicleCode, String createdBy) {
        logisticsEventService.recordEvent(
                transportRequestCode,
                taskCode,
                vehicleCode,
                LogisticsEventType.TRANSPORT_CANCELLED,
                "Transport cancelled",
                createdBy
        );
    }
}
