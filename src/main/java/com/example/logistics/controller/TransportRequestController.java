package com.example.logistics.controller;

import com.example.logistics.dto.request.TransportRequestCreateDto;
import com.example.logistics.dto.response.TransportRequestResponseDto;
import com.example.logistics.service.TransportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/logistics/requests")
@RequiredArgsConstructor
@Slf4j
public class TransportRequestController {

    private final TransportRequestService transportRequestService;

    /**
     * Creates a new transport request.
     * The farmerId is extracted from the API Gateway header "X-User-Id".
     */
    @PostMapping
    public ResponseEntity<TransportRequestResponseDto> createTransportRequest(
            @RequestHeader(value = "X-User-Id", required = true) UUID farmerId,
            @Valid @RequestBody TransportRequestCreateDto createDto) {
        
        log.info("Received request to create transport request from farmer: {}", farmerId);
        TransportRequestResponseDto responseDto = transportRequestService.createTransportRequest(farmerId, createDto);
        
        // Note: Wrapped in standard ResponseEntity. Modify to use ApiResponse wrapper if your team requires it at Controller level.
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Retrieves a transport request by its public code.
     */
    @GetMapping("/{requestCode}")
    public ResponseEntity<TransportRequestResponseDto> getTransportRequest(
            @PathVariable("requestCode") String requestCode) {
        
        log.info("Fetching transport request details for code: {}", requestCode);
        TransportRequestResponseDto responseDto = transportRequestService.getTransportRequestByCode(requestCode);
        
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Updates the status of an existing transport request.
     */
    @PatchMapping("/{requestCode}/status")
    public ResponseEntity<TransportRequestResponseDto> updateTransportRequestStatus(
            @PathVariable("requestCode") String requestCode,
            @RequestParam("status") String status) {
        
        log.info("Updating status for transport request: {} to {}", requestCode, status);
        TransportRequestResponseDto responseDto = transportRequestService.updateTransportRequestStatus(requestCode, status);
        
        return ResponseEntity.ok(responseDto);
    }
}
