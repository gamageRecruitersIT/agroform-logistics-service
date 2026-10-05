package com.example.logistics.controller;


import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.request.CostPreviewRequest;
import com.example.logistics.dto.response.CostPreviewResponse;
import com.example.logistics.service.impl.TransportCostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/logistics/cost")
@RequiredArgsConstructor
public class CostController {

    private final TransportCostService transportCostService;

    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<CostPreviewResponse>> previewCost(
            @Valid @RequestBody CostPreviewRequest request) {

        CostPreviewResponse previewResponse = transportCostService.calculateCostPreview(request);

        ApiResponse<CostPreviewResponse> response = new ApiResponse<>(
                true,
                "Transport cost calculated successfully",
                previewResponse
        );

        return ResponseEntity.ok(response);
    }
}