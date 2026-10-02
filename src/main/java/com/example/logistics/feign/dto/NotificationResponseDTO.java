package com.example.logistics.feign.dto;

import java.time.Instant;
import java.util.UUID;

// Acknowledgement returned by Communication & Support Service after a notification
// request is accepted. Delivery itself is async on their side — this just confirms
// the request was queued.
public record NotificationResponseDTO(
        UUID notificationId,
        String status,
        Instant sentAt
) {}
