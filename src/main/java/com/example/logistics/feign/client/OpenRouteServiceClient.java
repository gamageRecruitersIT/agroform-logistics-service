package com.example.logistics.feign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "openRouteServiceClient", url = "${ors.api.url}")
public interface OpenRouteServiceClient {

    @GetMapping("/v2/directions/driving-car")
    String getDirections(
                          @RequestParam("api_key") String apiKey,
                          @RequestParam("start") String start,
                          @RequestParam("end") String end
    );
}