package com.example.logistics.controller;

import com.example.logistics.dto.response.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Manual verification for role-based @PreAuthorize wiring across the logistics
// domain's three roles (see task list section 67-73).
@RestController
@RequestMapping("/api/test")
public class TestAuthController {

    @GetMapping("/transporter")
    @PreAuthorize("hasRole('TRANSPORTER')")
    public ApiResponse<String> transporterTest() {
        return ApiResponse.success("Transporter authorization successful", "You are authorized as TRANSPORTER");
    }

    @GetMapping("/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ApiResponse<String> driverTest() {
        return ApiResponse.success("Driver authorization successful", "You are authorized as DRIVER");
    }

    @GetMapping("/farmer")
    @PreAuthorize("hasRole('FARMER')")
    public ApiResponse<String> farmerTest() {
        return ApiResponse.success("Farmer authorization successful", "You are authorized as FARMER");
    }
}
