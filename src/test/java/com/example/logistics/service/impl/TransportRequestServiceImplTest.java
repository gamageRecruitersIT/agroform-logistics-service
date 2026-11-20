package com.example.logistics.service.impl;

import com.example.logistics.dto.request.TransportRequestCreateDto;
import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.entity.TransportRequest;
import com.example.logistics.feign.client.OrderPaymentServiceClient;
import com.example.logistics.feign.dto.OrderSummaryDto;
import com.example.logistics.repository.TransportRequestRepository;
import com.example.logistics.service.LogisticsWorkflowService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransportRequestServiceImplTest {

    @Test
    void flushesRequestBeforeRecordingCreatedEvent() {
        TransportRequestRepository requestRepository = mock(TransportRequestRepository.class);
        OrderPaymentServiceClient orderClient = mock(OrderPaymentServiceClient.class);
        LogisticsWorkflowService workflowService = mock(LogisticsWorkflowService.class);
        UUID orderId = UUID.randomUUID();
        UUID farmerId = UUID.randomUUID();

        when(requestRepository.existsByOrderId(orderId)).thenReturn(false);
        when(orderClient.getOrderDetails(orderId)).thenReturn(
                ApiResponse.success("Order found", OrderSummaryDto.builder()
                        .auctionRefCode("AUC-123")
                        .productName("Rice")
                        .quantity(java.math.BigDecimal.TEN)
                        .quantityUnit("KG")
                        .build())
        );
        when(requestRepository.saveAndFlush(any(TransportRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransportRequestServiceImpl service = new TransportRequestServiceImpl(
                requestRepository, orderClient, workflowService
        );
        TransportRequestCreateDto request = TransportRequestCreateDto.builder()
                .orderId(orderId)
                .build();

        var response = service.createTransportRequest(farmerId, request);

        assertNotNull(response.getTransportRequestCode());
        InOrder creationOrder = inOrder(requestRepository, workflowService);
        creationOrder.verify(requestRepository).saveAndFlush(any(TransportRequest.class));
        creationOrder.verify(workflowService).onTransportRequestCreated(
                anyString(), eq(farmerId.toString())
        );
    }
}
