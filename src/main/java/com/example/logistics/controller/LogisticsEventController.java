package com.example.logistics.controller;

import com.example.logistics.dto.request.LogisticsEventRequest;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.LogisticsEventResponse;
import com.example.logistics.enums.LogisticsEventType;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.service.LogisticsEventService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logistics/events")
public class LogisticsEventController {

    private final ObjectProvider<LogisticsEventService> eventService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> recordEvent(@Valid @RequestBody LogisticsEventRequest request) {
        LogisticsEventType eventType;
        try {
            eventType = LogisticsEventType.valueOf(request.eventType().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported logistics event type: " + request.eventType());
        }

        eventService.getObject().recordEvent(
                request.transportRequestCode(),
                request.taskCode(),
                request.vehicleCode(),
                eventType,
                request.description(),
                request.createdBy()
        );

        return ResponseEntity.ok(ApiResponse.success("Logistics event recorded successfully"));
    }

    @GetMapping("/transport-requests/{transportRequestCode}")
    public ResponseEntity<ApiResponse<List<LogisticsEventResponse>>> getTransportRequestHistory(
            @PathVariable String transportRequestCode
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Logistics event history retrieved successfully",
                eventService.getObject().getTransportRequestHistory(transportRequestCode)
        ));
    }
}