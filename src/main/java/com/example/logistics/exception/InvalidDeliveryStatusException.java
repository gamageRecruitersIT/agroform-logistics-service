package com.example.logistics.exception;

public class InvalidDeliveryStatusException extends RuntimeException {

    public InvalidDeliveryStatusException(String message) {
        super(message);
    }

    public InvalidDeliveryStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
