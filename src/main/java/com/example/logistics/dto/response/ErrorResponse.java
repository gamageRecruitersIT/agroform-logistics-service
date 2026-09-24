package com.example.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
    private boolean success;
    private String message;
    private int status;
    private String path;
    private LocalDateTime timestamp;

    public static ErrorResponse of(String message, int status, String path) {
        return new ErrorResponse(
                false,
                message,
                status,
                path,
                LocalDateTime.now()
        );
    }
}
