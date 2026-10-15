package com.example.logistics;

import com.example.logistics.repository.DeliveryStatusRepository;
import com.example.logistics.repository.DriverAssignmentRepository;
import com.example.logistics.repository.LogisticsEventRepository;
import com.example.logistics.repository.LogisticsReportRepository;
import com.example.logistics.repository.RouteEstimateRepository;
import com.example.logistics.repository.TrackingUpdateRepository;
import com.example.logistics.repository.TransportCostRuleRepository;
import com.example.logistics.repository.TransportRequestRepository;
import com.example.logistics.repository.TransportTaskRepository;
import com.example.logistics.repository.TransportTaskLookupRepository;
import com.example.logistics.repository.VehiclePhotoRepository;
import com.example.logistics.repository.VehicleRepository;
import com.example.logistics.service.LogisticsWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
})
class LogisticsServiceApplicationTests {

    @MockitoBean
    private DeliveryStatusRepository deliveryStatusRepository;

    @MockitoBean
    private DriverAssignmentRepository driverAssignmentRepository;

    @MockitoBean
    private LogisticsEventRepository logisticsEventRepository;

    @MockitoBean
    private LogisticsReportRepository logisticsReportRepository;

    @MockitoBean
    private RouteEstimateRepository routeEstimateRepository;

    @MockitoBean
    private TrackingUpdateRepository trackingUpdateRepository;

    @MockitoBean
    private TransportCostRuleRepository transportCostRuleRepository;

    @MockitoBean
    private TransportRequestRepository transportRequestRepository;

    @MockitoBean
    private TransportTaskRepository transportTaskRepository;

    @MockitoBean
    private TransportTaskLookupRepository transportTaskLookupRepository;

    @MockitoBean
    private VehiclePhotoRepository vehiclePhotoRepository;

    @MockitoBean
    private VehicleRepository vehicleRepository;

    @MockitoBean
    private LogisticsWorkflowService logisticsWorkflowService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }
}
