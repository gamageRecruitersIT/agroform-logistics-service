package com.example.logistics.entity.enums;

public enum VehicleAvailability {
    AVAILABLE,
    ASSIGNED,
    UNDER_MAINTENANCE,
    INACTIVE;


    public static VehicleAvailability fromApi(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be empty");
        }

        String upperStatus = status.trim().toUpperCase();

        if (upperStatus.equals("ACTIVE")) {
            return AVAILABLE;
        }

        try {
            return VehicleAvailability.valueOf(upperStatus);
        } catch (IllegalArgumentException e) {

            throw new IllegalArgumentException("Invalid vehicle status: " + status);
        }
    }
}