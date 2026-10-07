package com.example.logistics.service;

import com.example.logistics.dto.response.DistrictPerformanceResponse;
import com.example.logistics.dto.response.LogisticsSummaryResponse;

import java.util.List;

public interface LogisticsReportService {

    LogisticsSummaryResponse getSummary();

    List<DistrictPerformanceResponse> getDistrictPerformance();
}