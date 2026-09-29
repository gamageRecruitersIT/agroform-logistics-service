package com.example.logistics.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportRequestCreateDto {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Auction reference code is required")
    private String auctionRefCode;

    @NotBlank(message = "Product name is required")
    private String productName;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private String quantityUnit = "KG";

    private BigDecimal pickupLatitude;

    private BigDecimal pickupLongitude;

    private String pickupAddress;

    private UUID warehouseId;

    private BigDecimal warehouseLatitude;

    private BigDecimal warehouseLongitude;

    private String notes;
}
