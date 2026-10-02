package com.example.logistics.service;

public interface LogisticsWorkflowService {

    void onTransportRequestCreated(String transportRequestCode, String createdBy);

    void onVehicleAssigned(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onDriverAssigned(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onPickupStarted(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onInTransit(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onDelivered(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onUnloadedAtWarehouse(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);

    void onTransportCancelled(String transportRequestCode, String taskCode, String vehicleCode, String createdBy);
}
