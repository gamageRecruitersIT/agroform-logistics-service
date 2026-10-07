package com.example.logistics.service.impl;

import com.example.logistics.dto.response.DistrictPerformanceResponse;
import com.example.logistics.dto.response.LogisticsSummaryResponse;
import com.example.logistics.repository.LogisticsReportRepository;
import com.example.logistics.service.LogisticsReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(LogisticsReportRepository.class)
public class LogisticsReportServiceImpl implements LogisticsReportService {

    private final LogisticsReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public LogisticsSummaryResponse getSummary() {
        return reportRepository.findSummary();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DistrictPerformanceResponse> getDistrictPerformance() {
        return reportRepository.findDistrictPerformance();
    }
}