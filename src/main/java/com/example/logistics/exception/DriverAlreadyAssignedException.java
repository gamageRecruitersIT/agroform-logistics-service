package com.example.logistics.exception;

public class DriverAlreadyAssignedException extends RuntimeException {

    public DriverAlreadyAssignedException(String message) {
        super(message);
    }

    public DriverAlreadyAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
