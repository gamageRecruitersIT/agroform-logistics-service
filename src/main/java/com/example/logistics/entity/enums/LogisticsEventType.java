package com.example.logistics.enums;

import java.util.Arrays;

public enum LogisticsEventType {
    TRANSPORT_REQUEST_CREATED("TRANSPORT_REQUEST_CREATED"),
    VEHICLE_ASSIGNED("VEHICLE_UPDATED"),
    DRIVER_ASSIGNED("DRIVER_ASSIGNED_TO_TASK"),
    PICKUP_STARTED("DELIVERY_STATUS_UPDATED"),
    IN_TRANSIT("DELIVERY_STATUS_UPDATED"),
    DELIVERED("DELIVERY_COMPLETED"),
    UNLOADED_AT_WAREHOUSE("DELIVERY_COMPLETED"),
    TRANSPORT_CANCELLED("TASK_CANCELLED");

    private final String legacyValue;

    LogisticsEventType(String legacyValue) {
        this.legacyValue = legacyValue;
    }

    public String getLegacyValue() {
        return legacyValue;
    }

    public static LogisticsEventType fromLegacyValue(String legacyValue) {
        if (legacyValue == null || legacyValue.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(legacyValue)
                        || type.legacyValue.equalsIgnoreCase(legacyValue))
                .findFirst()
                .orElse(null);
    }
}