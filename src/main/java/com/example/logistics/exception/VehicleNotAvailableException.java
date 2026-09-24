package com.example.logistics.exception;

public class VehicleNotAvailableException extends RuntimeException {

    public VehicleNotAvailableException(String message) {
        super(message);
    }

    public VehicleNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
