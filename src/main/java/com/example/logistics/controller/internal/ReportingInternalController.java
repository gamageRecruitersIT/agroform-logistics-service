package com.example.logistics.controller.internal;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.TransportReportData.*;
import com.example.logistics.exception.UnauthorizedException;
import com.example.logistics.service.TransportReportQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Internal, service-to-service endpoints for Monitoring & Reporting (Feign).
 * Base path: /api/v1/logistics/internal/reports
 * Every call must carry the shared key in the X-Internal-Service-Key header.
 * Window: transport_request.created_at in [from, to), ISO-8601 date-time, e.g. 2026-11-01T00:00:00+05:30.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logistics/internal/reports")
public class ReportingInternalController {

    private static final String KEY_HEADER = "X-Internal-Service-Key";

    private final TransportReportQueryService service;

    @Value("${internal.service-key:}")
    private String expectedKey;

    @GetMapping("/transport-summary")
    public ResponseEntity<ApiResponse<TransportSummary>> transportSummary(
            @RequestHeader(value = KEY_HEADER, required = false) String key,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String district) {
        authorise(key);
        return ResponseEntity.ok(ApiResponse.success("Transport summary retrieved",
                service.summary(from, to, district)));
    }

    @GetMapping("/delivery-performance")
    public ResponseEntity<ApiResponse<DeliveryPerformance>> deliveryPerformance(
            @RequestHeader(value = KEY_HEADER, required = false) String key,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String district) {
        authorise(key);
        return ResponseEntity.ok(ApiResponse.success("Delivery performance retrieved",
                service.deliveryPerformance(from, to, district)));
    }

    @GetMapping("/delay-summary")
    public ResponseEntity<ApiResponse<DelaySummary>> delaySummary(
            @RequestHeader(value = KEY_HEADER, required = false) String key,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String district) {
        authorise(key);
        return ResponseEntity.ok(ApiResponse.success("Delay summary retrieved",
                service.delaySummary(from, to, district)));
    }

    @GetMapping("/district-breakdown")
    public ResponseEntity<ApiResponse<List<DistrictBreakdown>>> districtBreakdown(
            @RequestHeader(value = KEY_HEADER, required = false) String key,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        authorise(key);
        return ResponseEntity.ok(ApiResponse.success("District breakdown retrieved",
                service.districtBreakdown(from, to)));
    }

    // Fails closed: if no key is configured, every call is rejected.
    private void authorise(String provided) {
        if (expectedKey == null || expectedKey.isBlank() || provided == null
                || !MessageDigest.isEqual(expectedKey.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedException("Invalid or missing internal service key.");
        }
    }
}
