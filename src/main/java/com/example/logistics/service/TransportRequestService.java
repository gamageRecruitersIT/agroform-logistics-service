package com.example.logistics.service;

import com.example.logistics.dto.request.TransportRequestCreateDto;
import com.example.logistics.dto.response.TransportRequestResponseDto;

import java.util.UUID;

public interface TransportRequestService {

    /**
     * Creates a new transport request after verifying the order.
     *
     * @param farmerId The ID of the farmer creating the request (extracted from JWT).
     * @param createDto The request details.
     * @return TransportRequestResponseDto
     */
    TransportRequestResponseDto createTransportRequest(UUID farmerId, TransportRequestCreateDto createDto);

    /**
     * Gets a transport request by its public code.
     *
     * @param requestCode The transport request code.
     * @return TransportRequestResponseDto
     */
    TransportRequestResponseDto getTransportRequestByCode(String requestCode);

    /**
     * Updates the status of a transport request.
     *
     * @param requestCode The transport request code.
     * @param newStatus The new status string.
     * @return TransportRequestResponseDto
     */
    TransportRequestResponseDto updateTransportRequestStatus(String requestCode, String newStatus);
}
