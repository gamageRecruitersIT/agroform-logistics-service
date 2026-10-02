package com.example.logistics.service.impl;

import com.example.logistics.dto.request.TransportRequestCreateDto;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.TransportRequestResponseDto;
import com.example.logistics.entity.TransportRequest;
import com.example.logistics.entity.enums.TransportRequestStatusEnum;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.exception.DuplicateResourceException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.feign.client.OrderPaymentServiceClient;
import com.example.logistics.feign.dto.OrderSummaryDto;
import com.example.logistics.repository.TransportRequestRepository;
import com.example.logistics.service.TransportRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransportRequestServiceImpl implements TransportRequestService {

    private final TransportRequestRepository transportRequestRepository;
    private final OrderPaymentServiceClient orderPaymentServiceClient;

    @Override
    @Transactional
    public TransportRequestResponseDto createTransportRequest(UUID farmerId, TransportRequestCreateDto dto) {
        log.info("Creating transport request for order: {} by farmer: {}", dto.getOrderId(), farmerId);

        // 1. Idempotency Check (Prevent duplicate requests for the same order)
        if (transportRequestRepository.existsByOrderId(dto.getOrderId())) {
            throw new DuplicateResourceException("A transport request already exists for Order ID: " + dto.getOrderId());
        }

        // 2. Verify Order via Feign Client
        OrderSummaryDto orderSummary = null;
        try {
            ApiResponse<OrderSummaryDto> orderResponse = orderPaymentServiceClient.getOrderDetails(dto.getOrderId());
            if (orderResponse == null || orderResponse.getData() == null) {
                throw new BadRequestException("Failed to retrieve valid order details from Order Service.");
            }
            orderSummary = orderResponse.getData();
        } catch (Exception e) {
            log.error("Error communicating with Order Payment Service", e);
            throw new BadRequestException("Order validation failed. Reason: " + e.getMessage());
        }

        // 3. Generate Request Code & Map Entity
        String generatedCode = "TRQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        TransportRequest transportRequest = TransportRequest.builder()
                .transportRequestCode(generatedCode)
                .farmerId(farmerId)
                .orderId(dto.getOrderId())
                .auctionRefCode(orderSummary != null ? orderSummary.getAuctionRefCode() : null)
                .productName(orderSummary != null ? orderSummary.getProductName() : "Unknown")
                .quantity(orderSummary != null ? orderSummary.getQuantity() : java.math.BigDecimal.ZERO)
                .quantityUnit(orderSummary != null && orderSummary.getQuantityUnit() != null ? orderSummary.getQuantityUnit() : "KG")
                .pickupLatitude(dto.getPickupLatitude())
                .pickupLongitude(dto.getPickupLongitude())
                .pickupAddress(dto.getPickupAddress())
                .warehouseId(dto.getWarehouseId())
                .warehouseLatitude(dto.getWarehouseLatitude())
                .warehouseLongitude(dto.getWarehouseLongitude())
                .requestStatus(TransportRequestStatusEnum.PENDING)
                .notes(dto.getNotes())
                .build();

        // 4. Save Entity to Database
        TransportRequest savedRequest = transportRequestRepository.save(transportRequest);

        // 5. TODO: Call Gayani's LogisticsEventPublisher here to publish TRANSPORT_REQUEST_CREATED event

        return mapToResponseDto(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public TransportRequestResponseDto getTransportRequestByCode(String requestCode) {
        TransportRequest request = transportRequestRepository.findByTransportRequestCode(requestCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transport request not found with code: " + requestCode));
        
        return mapToResponseDto(request);
    }

    @Override
    @Transactional
    public TransportRequestResponseDto updateTransportRequestStatus(String requestCode, String newStatus) {
        TransportRequest request = transportRequestRepository.findByTransportRequestCode(requestCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transport request not found with code: " + requestCode));

        try {
            TransportRequestStatusEnum statusEnum = TransportRequestStatusEnum.valueOf(newStatus.toUpperCase());
            request.setRequestStatus(statusEnum);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid transport request status: " + newStatus);
        }

        TransportRequest updatedRequest = transportRequestRepository.save(request);

        // TODO: Publish event for status update via Gayani's event module

        return mapToResponseDto(updatedRequest);
    }

    private TransportRequestResponseDto mapToResponseDto(TransportRequest entity) {
        return TransportRequestResponseDto.builder()
                .transportRequestId(entity.getTransportRequestId())
                .transportRequestCode(entity.getTransportRequestCode())
                .farmerId(entity.getFarmerId())
                .orderId(entity.getOrderId())
                .auctionRefCode(entity.getAuctionRefCode())
                .productName(entity.getProductName())
                .quantity(entity.getQuantity())
                .quantityUnit(entity.getQuantityUnit())
                .estimatedDistanceKm(entity.getEstimatedDistanceKm())
                .requestStatus(entity.getRequestStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
