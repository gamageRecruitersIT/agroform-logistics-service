package com.example.logistics.feign.client;

import com.example.logistics.dto.response.ApiResponse;
import com.example.logistics.feign.dto.OrderSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "order-payment-service", url = "${order-payment.service.url:http://localhost:8070}")
public interface OrderPaymentServiceClient {

    /**
     * Calls the Order & Payment Service to verify the order details.
     * 
     * @param orderId the UUID of the order
     * @return ApiResponse containing the OrderSummaryDto
     */
    @GetMapping("/api/v1/payment/order/id/{orderId}")
    ApiResponse<OrderSummaryDto> getOrderDetails(@PathVariable("orderId") UUID orderId);
}
