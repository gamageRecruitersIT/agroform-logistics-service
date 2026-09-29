package com.example.logistics.feign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {
    
    private UUID orderId;
    private String auctionRefCode;
    private UUID farmerId;
    private UUID buyerId;
    private String paymentStatus;
    
    // New fields from Order Service
    private String productName;
    private java.math.BigDecimal quantity;
    private String quantityUnit;
    
}
