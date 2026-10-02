package com.example.logistics.feign.dto;

import java.util.UUID;

// Response from Identity & Access Service used to verify transporter/driver eligibility
// (active status, correct role) before a driver/vehicle assignment is confirmed.
public record UserStatusDto(
        UUID userId,
        String userCode,
        String userStatus,
        String roleCode
) {}
