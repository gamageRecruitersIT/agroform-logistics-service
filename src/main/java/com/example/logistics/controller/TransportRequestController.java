package com.example.logistics.controller;

import org.springframework.security.access.prepost.PreAuthorize;

import com.example.logistics.dto.request.TransportRequestCreateDto;
import com.example.logistics.dto.response.TransportRequestResponseDto;
import com.example.logistics.security.CurrentUser;
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
     * The farmerId is the authenticated caller's id (JWT subject).
     */
    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<TransportRequestResponseDto> createTransportRequest(
            CurrentUser user,
            @Valid @RequestBody TransportRequestCreateDto createDto) {
        
        log.info("Received request to create transport request from farmer: {}", user.userId());
        TransportRequestResponseDto responseDto = transportRequestService.createTransportRequest(user.userId(), createDto);
        
        // Note: Wrapped in standard ResponseEntity. Modify to use ApiResponse wrapper if your team requires it at Controller level.
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Retrieves a transport request by its public code.
     */
    @GetMapping("/{requestCode}")
    @PreAuthorize("hasAnyRole('FARMER','TRANSPORTER','DRIVER')")
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
    @PreAuthorize("hasAnyRole('TRANSPORTER','FARMER')")
    public ResponseEntity<TransportRequestResponseDto> updateTransportRequestStatus(
            @PathVariable("requestCode") String requestCode,
            @RequestParam("status") String status) {
        
        log.info("Updating status for transport request: {} to {}", requestCode, status);
        TransportRequestResponseDto responseDto = transportRequestService.updateTransportRequestStatus(requestCode, status);
        
        return ResponseEntity.ok(responseDto);
    }
}
