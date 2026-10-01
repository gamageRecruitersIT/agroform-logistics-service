package com.example.logistics.service.impl;

import com.example.logistics.entity.RouteEstimate;
import com.example.logistics.entity.TransportCostRule;
import com.example.logistics.entity.TransportRequest;
import com.example.logistics.dto.request.CostPreviewRequest;
import com.example.logistics.dto.response.CostPreviewResponse;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.repository.RouteEstimateRepository;
import com.example.logistics.repository.TransportCostRuleRepository;
import com.example.logistics.repository.TransportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class TransportCostService {

    private final TransportCostRuleRepository costRuleRepository;
    private final RouteEstimateRepository routeEstimateRepository;
    private final ExternalMapService externalMapService;
    private final TransportRequestRepository transportRequestRepository;

    @Transactional
    public CostPreviewResponse calculateCostPreview(CostPreviewRequest request) {

        if (request.getProductWeight() == null || request.getProductWeight() <= 0) {
            throw new BadRequestException("Invalid product weight. Weight must be greater than 0.");
        }
        if (request.getOriginLat() == null || request.getDestinationLat() == null) {
            throw new BadRequestException("Origin and Destination coordinates are required.");
        }

        double[] routeData = externalMapService.getDistanceAndDuration(
                request.getOriginLat(), request.getOriginLng(),
                request.getDestinationLat(), request.getDestinationLng()
        );

        double actualDistanceKm = routeData[0];
        int estimatedTimeMinutes = (int) Math.round(routeData[1]);

        if (actualDistanceKm <= 0) {
            throw new BadRequestException("Could not calculate a valid route distance.");
        }


        if (request.getTransportRequestId() != null) {
            TransportRequest transportRequest = transportRequestRepository.findById(request.getTransportRequestId())
                    .orElseThrow(() -> new RuntimeException("Transport Request not found"));


            transportRequest.setEstimatedDistanceKm(BigDecimal.valueOf(actualDistanceKm));
            transportRequestRepository.save(transportRequest);
        }

        TransportCostRule rule = costRuleRepository.findByTransporterIdAndIsActiveTrue(request.getTransporterId())
                .orElseThrow(() -> new BadRequestException("Cost rules are not configured for transporter: " + request.getTransporterId()));

        // Cost calculate : (Distance * CostPerKm) + (Weight * CostPerKg)
        BigDecimal distanceCost = rule.getCostPerKm().multiply(BigDecimal.valueOf(actualDistanceKm));
        BigDecimal weightCost = rule.getCostPerKg().multiply(BigDecimal.valueOf(request.getProductWeight()));

        // Total cost
        BigDecimal finalCost = distanceCost.add(weightCost);
        finalCost = finalCost.setScale(2, RoundingMode.HALF_UP);

        RouteEstimate estimate = new RouteEstimate();
        estimate.setEstimatedDistanceKm(actualDistanceKm);
        estimate.setWeightUsed(request.getProductWeight());
        estimate.setEstimatedCost(finalCost);
        estimate.setCostPerKmUsed(rule.getCostPerKm());
        estimate.setCostPerKgUsed(rule.getCostPerKg());
        estimate.setTransportRequestId(request.getTransportRequestId());

        RouteEstimate savedEstimate = routeEstimateRepository.save(estimate);

        String generatedMapUrl = String.format("https://www.google.com/maps/dir/?api=1&origin=%f,%f&destination=%f,%f",
                request.getOriginLat(), request.getOriginLng(),
                request.getDestinationLat(), request.getDestinationLng());

        return CostPreviewResponse.builder()
                .routeEstCode(savedEstimate.getRouteEstimateId() != null ? savedEstimate.getRouteEstimateId().toString() : "")
                .estimatedCost(savedEstimate.getEstimatedCost())
                .distanceKm(savedEstimate.getEstimatedDistanceKm())
                .etaMinutes(estimatedTimeMinutes)
                .routeMapUrl(generatedMapUrl)
                .build();
    }
}