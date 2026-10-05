package com.example.logistics.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class CostPreviewResponse {
    private String routeEstCode;
    private BigDecimal estimatedCost;
    private Double distanceKm;
    private Integer etaMinutes;
    private String routeMapUrl;
}