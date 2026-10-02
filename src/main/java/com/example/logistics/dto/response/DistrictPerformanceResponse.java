package com.example.logistics.dto.response;

import java.math.BigDecimal;

public record DistrictPerformanceResponse(
        String district,
        long totalTransportRequests,
        long completedTransportRequests,
        BigDecimal averageDistanceKm
) {
}