package com.example.logistics.service;

import com.example.logistics.dto.response.TransportReportData.*;
import com.example.logistics.exception.BadRequestException;
import com.example.logistics.repository.TransportReportQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransportReportQueryService {

    private final TransportReportQueryRepository repository;

    public TransportSummary summary(OffsetDateTime from, OffsetDateTime to, String district) {
        return repository.summary(window(from, to, district));
    }

    public DeliveryPerformance deliveryPerformance(OffsetDateTime from, OffsetDateTime to, String district) {
        return repository.deliveryPerformance(window(from, to, district));
    }

    public DelaySummary delaySummary(OffsetDateTime from, OffsetDateTime to, String district) {
        return repository.delaySummary(window(from, to, district));
    }

    public List<DistrictBreakdown> districtBreakdown(OffsetDateTime from, OffsetDateTime to) {
        validate(from, to);
        return repository.districtBreakdown(from, to);
    }

    private Window window(OffsetDateTime from, OffsetDateTime to, String district) {
        validate(from, to);
        String d = (district == null || district.isBlank()) ? null : district.trim();
        return new Window(from, to, d, OffsetDateTime.now());
    }

    private void validate(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) {
            throw new BadRequestException("Both 'from' and 'to' are required (ISO-8601 date-time).");
        }
        if (!to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'. The window is [from, to).");
        }
    }
}
