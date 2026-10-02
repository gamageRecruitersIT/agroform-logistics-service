package com.example.logistics.dto.response;

public record LogisticsSummaryResponse(
        long totalTransportRequests,
        long pendingTransportRequests,
        long acceptedTransportRequests,
        long completedTransportRequests,
        long cancelledTransportRequests,
        long totalLogisticsEvents
) {
}