package com.example.logistics.exception;

import com.example.logistics.dto.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(ApiResponse.error(msg));
    }

    // 400 - invalid input / business rule violation
    @ExceptionHandler({BadRequestException.class, InvalidDeliveryStatusException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 409 - duplicates / vehicle or driver conflicts
    @ExceptionHandler({DuplicateResourceException.class, VehicleNotAvailableException.class,
            DriverAlreadyAssignedException.class})
    public ResponseEntity<ApiResponse<Object>> handleConflict(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 403 - TRANSPORTER / DRIVER / FARMER role checks
    @ExceptionHandler({ForbiddenException.class, UnauthorizedRoleException.class})
    public ResponseEntity<ApiResponse<Object>> handleForbidden(RuntimeException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // @PreAuthorize denials: rethrow so Spring Security answers (401 anonymous via
    // RestAuthenticationEntryPoint, 403 authenticated via RestAccessDeniedHandler)
    // instead of the generic handler below masking them.
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) {
        throw ex;
    }

    // 400 - @Valid body errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return build(HttpStatus.BAD_REQUEST, msg.isBlank() ? "Validation failed" : msg);
    }

    // 400 - @Validated path/query param errors
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - bad JSON / wrong param type
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Object>> handleMalformed(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request or invalid parameter");
    }

    // 409 - DB constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return build(HttpStatus.CONFLICT, "Data conflict: duplicate or invalid reference");
    }

    // Feign: reflect the downstream status instead of masking everything as 503
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleFeign(FeignException ex) {
        int status = ex.status();
        log.error("Downstream call failed: status={}, message={}", status, ex.getMessage());

        if (status == 400 || status == 404 || status == 409) {
            return build(HttpStatus.valueOf(status), downstreamMessage(ex));
        }
        if (status == 401 || status == 403) {
            return build(HttpStatus.valueOf(status), "Dependent service rejected the request: " + downstreamMessage(ex));
        }
        if (status >= 500) {
            return build(HttpStatus.BAD_GATEWAY, "Dependent service failed to process the request");
        }
        if (ex.getCause() instanceof java.net.SocketTimeoutException) {
            return build(HttpStatus.GATEWAY_TIMEOUT, "Dependent service timed out");
        }
        // status -1 (connection refused / unknown host) or unexpected
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A dependent service is currently unavailable");
    }

    // 503 - explicit downstream failures raised by our own code
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleExternal(ExternalServiceException ex) {
        log.error("Downstream service error", ex);
        return build(HttpStatus.SERVICE_UNAVAILABLE, "A dependent service is currently unavailable");
    }

    // Both services return a JSON "message" field; fall back to a generic text.
    private String downstreamMessage(FeignException ex) {
        try {
            String msg = objectMapper.readTree(ex.contentUTF8()).path("message").asText("");
            if (!msg.isBlank()) return msg;
        } catch (Exception ignored) {
            // empty / non-JSON body
        }
        return "Request to dependent service failed";
    }

    // 500 - fallback (details only in logs, not sent to client)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
