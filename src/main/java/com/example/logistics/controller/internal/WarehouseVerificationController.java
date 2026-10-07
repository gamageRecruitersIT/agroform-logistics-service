package com.example.logistics.controller.internal;

import com.example.logistics.dto.request.WarehouseReConfirmRequest;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.WarehouseReConfirmResponse;
import com.example.logistics.service.WarehouseVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal endpoint for Warehouse & Inventory Service to re-confirm a delivery.
 * Base path: /api/v1/logistics/internal/warehouse-verification
 * Not exposed to end users — internal service-to-service only.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logistics/internal/warehouse-verification")
public class WarehouseVerificationController {

    private final WarehouseVerificationService warehouseVerificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseReConfirmResponse>> reConfirmDelivery(
            @Valid @RequestBody WarehouseReConfirmRequest request
    ) {
        WarehouseReConfirmResponse result =
                warehouseVerificationService.handleWarehouseReConfirmation(request);

        return ResponseEntity.ok(
                ApiResponse.success("Warehouse re-confirmation processed successfully.", result)
        );
    }
}
