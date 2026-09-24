package com.example.logistics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {
    private boolean success;
    private String message;
    private int status;
    private String path;
    private Map<String, String> errors;
    private LocalDateTime timestamp;

    public static ValidationErrorResponse of(
            String message,
            int status,
            String path,
            Map<String, String> errors
    ) {
        return new ValidationErrorResponse(
                false,
                message,
                status,
                path,
                errors,
                LocalDateTime.now()
        );
    }
}
