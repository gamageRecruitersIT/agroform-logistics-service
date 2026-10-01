package com.example.logistics.feign.client;

import com.example.logistics.feign.dto.WarehouseDeliveryNotificationRequest;
import com.example.logistics.feign.dto.WarehouseDeliveryNotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

// url loaded from application.yml: warehouse.service.url
@FeignClient(
        name = "warehouse-service",
        url  = "${warehouse.service.url}"
)
public interface WarehouseServiceFeignClient {

    @PostMapping("/api/v1/warehouse/internal/delivery-notification")
    WarehouseDeliveryNotificationResponse notifyDeliveryArrival(
            @RequestBody WarehouseDeliveryNotificationRequest request
    );
}
