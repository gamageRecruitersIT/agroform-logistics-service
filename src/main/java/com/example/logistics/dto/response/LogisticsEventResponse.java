package com.example.logistics.dto.response;

public record LogisticsEventResponse(

        String eventId,

        String eventType,

        String description,

        String createdAt

) {
}