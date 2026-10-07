package com.example.logistics.service.impl;

import com.example.logistics.dto.request.CostPreviewRequest;
import com.example.logistics.dto.response.CostPreviewResponse;
import com.example.logistics.entity.TransportCostRule;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.repository.RouteEstimateRepository;
import com.example.logistics.repository.TransportCostRuleRepository;
import com.example.logistics.repository.TransportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TransportCostService {

    private final TransportCostRuleRepository costRuleRepository;
    private final RouteEstimateRepository routeEstimateRepository;
    private final ExternalMapService externalMapService;
    private final TransportRequestRepository transportRequestRepository;


    public CostPreviewResponse calculateCostPreview(CostPreviewRequest request) {

        if (request.getProductWeight() == null || request.getProductWeight() <= 0) {
            throw new BadRequestException("Invalid product weight. Weight must be greater than 0.");
        }
        if (request.getOriginLat() == null || request.getDestinationLat() == null) {
            throw new BadRequestException("Origin and Destination coordinates are required.");
        }


        if (request.getTransportRequestId() != null) {
            boolean exists = transportRequestRepository.existsById(request.getTransportRequestId());
            if (!exists) {

                throw new ResourceNotFoundException("Transport Request not found with ID: " + request.getTransportRequestId());
            }
        }

        double[] routeData;
        try {
            routeData = externalMapService.getDistanceAndDuration(
                    request.getOriginLat(), request.getOriginLng(),
                    request.getDestinationLat(), request.getDestinationLng()
            );
        } catch (Exception e) {

            throw new RuntimeException("External map service unavailable: " + e.getMessage());
        }

        double actualDistanceKm = routeData[0];
        int estimatedTimeMinutes = (int) Math.round(routeData[1]);

        if (actualDistanceKm <= 0) {
            throw new BadRequestException("Could not calculate a valid route distance.");
        }

        TransportCostRule rule = costRuleRepository.findByTransporterIdAndIsActiveTrue(request.getTransporterId())
                .orElseThrow(() -> new BadRequestException("Cost rules are not configured for transporter: " + request.getTransporterId()));

        // 2. NullPointerException (NPE)  Null Checks
        BigDecimal costPerKm = rule.getCostPerKm() != null ? rule.getCostPerKm() : BigDecimal.ZERO;
        BigDecimal costPerKg = rule.getCostPerKg() != null ? rule.getCostPerKg() : BigDecimal.ZERO;

        // Cost calculate : (Distance * CostPerKm) + (Weight * CostPerKg)
        BigDecimal distanceCost = costPerKm.multiply(BigDecimal.valueOf(actualDistanceKm));
        BigDecimal weightCost = costPerKg.multiply(BigDecimal.valueOf(request.getProductWeight()));

        // Total cost
        BigDecimal finalCost = distanceCost.add(weightCost);
        finalCost = finalCost.setScale(2, RoundingMode.HALF_UP);

        String generatedMapUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f",
                request.getOriginLat(), request.getOriginLng(),
                request.getDestinationLat(), request.getDestinationLng());


        return CostPreviewResponse.builder()
                .routeEstCode("PREVIEW-ONLY")
                .estimatedCost(finalCost)
                .distanceKm(actualDistanceKm)
                .etaMinutes(estimatedTimeMinutes)
                .routeMapUrl(generatedMapUrl)
                .build();
    }
}