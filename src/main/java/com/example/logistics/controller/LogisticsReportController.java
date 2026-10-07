package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.DistrictPerformanceResponse;
import com.example.logistics.dto.response.LogisticsSummaryResponse;
import com.example.logistics.service.LogisticsReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/logistics/reports")
public class LogisticsReportController {

    private final ObjectProvider<LogisticsReportService> reportService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<LogisticsSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Logistics summary retrieved successfully",
                reportService.getObject().getSummary()
        ));
    }

    @GetMapping("/district-performance")
    public ResponseEntity<ApiResponse<List<DistrictPerformanceResponse>>> getDistrictPerformance() {
        return ResponseEntity.ok(ApiResponse.success(
                "District performance retrieved successfully",
                reportService.getObject().getDistrictPerformance()
        ));
    }
}