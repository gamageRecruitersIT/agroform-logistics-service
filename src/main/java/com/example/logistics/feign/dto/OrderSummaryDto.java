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
    private String paymentStatus; // e.g., PAID, FAILED, PENDING
    
}
