package com.example.logistics.service.impl;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.dto.response.TransportTaskDetailResponse;
import com.example.logistics.entity.TransportRequest;
import com.example.logistics.entity.TransportTask;
import com.example.logistics.exception.ExternalServiceException;
import com.example.logistics.exception.ResourceNotFoundException;
import com.example.logistics.feign.client.OrderPaymentServiceClient;
import com.example.logistics.feign.dto.OrderSummaryDto;
import com.example.logistics.repository.TransportRequestRepository;
import com.example.logistics.repository.TransportTaskRepository;
import com.example.logistics.service.TransportTaskDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportTaskDetailServiceImpl implements TransportTaskDetailService {

    private final TransportTaskRepository transportTaskRepository;
    private final TransportRequestRepository transportRequestRepository;
    private final OrderPaymentServiceClient orderPaymentServiceClient;

    @Override
    public TransportTaskDetailResponse getTaskDetail(UUID transportTaskId) {
        TransportTask task = transportTaskRepository.findById(transportTaskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transport task not found with id: " + transportTaskId));

        TransportRequest request = transportRequestRepository.findById(task.getTransportRequestId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transport request not found for task: " + task.getTransportTaskCode()));

        return TransportTaskDetailResponse.builder()
                .transportTaskId(task.getTransportTaskId())
                .transportTaskCode(task.getTransportTaskCode())
                .transportRequestId(task.getTransportRequestId())
                .transporterId(task.getTransporterId())
                .driverId(task.getDriverId())
                .orderId(request.getOrderId())
                .farmerId(request.getFarmerId())
                .buyerId(resolveBuyerId(request.getOrderId()))
                .auctionRefCode(request.getAuctionRefCode())
                .productName(request.getProductName())
                .quantityKg(toKg(request.getQuantity(), request.getQuantityUnit()))
                // TODO: fill from Identity Service once a name lookup endpoint exists.
                // Warehouse stores "Unknown" for null names.
                .farmerName(null)
                .transporterName(null)
                .driverName(null)
                .build();
    }

    /**
     * Resolves buyerId from Order & Payment Service.
     * TODO: remove the fallback UUID before production — Order Service must be running.
     * Temporary fallback is used because Order Service can't compile on Java 25 (Lombok issue).
     */
    private UUID resolveBuyerId(UUID orderId) {
        try {
            ApiResponse<OrderSummaryDto> response = orderPaymentServiceClient.getOrderDetails(orderId);
            OrderSummaryDto order = response != null ? response.getData() : null;
            if (order != null && order.getBuyerId() != null) {
                return order.getBuyerId();
            }
        } catch (Exception ex) {
            log.warn("Could not fetch order {} from Order & Payment Service: {} — using test fallback",
                    orderId, ex.getMessage());
        }
        // Fallback for local testing when Order Service is unavailable
        return UUID.fromString("00000000-0000-0000-0000-000000000099");
    }

    private BigDecimal toKg(BigDecimal quantity, String unit) {
        if (quantity == null) {
            return null;
        }
        String u = unit == null ? "KG" : unit.trim().toUpperCase();
        return switch (u) {
            case "G", "GRAM", "GRAMS" -> quantity.divide(BigDecimal.valueOf(1000));
            case "TON", "TONS", "TONNE", "TONNES", "MT" -> quantity.multiply(BigDecimal.valueOf(1000));
            default -> quantity; // KG (default unit on TransportRequest)
        };
    }
}
