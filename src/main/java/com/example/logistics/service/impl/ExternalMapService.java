package com.example.logistics.service.impl;


import com.example.logistics.feign.client.OpenRouteServiceClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExternalMapService {

    private final OpenRouteServiceClient orsClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ors.api.key}")
    private String apiKey;

    public double[] getDistanceAndDuration(double originLat, double originLng, double destLat, double destLng) {


        String startPoint = originLng + "," + originLat;
        String endPoint = destLng + "," + destLat;

        try {

            String jsonResponse = orsClient.getDirections(apiKey, startPoint, endPoint);

            JsonNode response = objectMapper.readTree(jsonResponse);

            JsonNode summary = response.path("features").get(0).path("properties").path("summary");

            double distanceKm = summary.path("distance").asDouble() / 1000.0;
            double durationMinutes = summary.path("duration").asDouble() / 60.0;


            return new double[]{distanceKm, durationMinutes};

        } catch (Exception e) {
            throw new RuntimeException("Map API Call Failed", e);
        }
    }
}