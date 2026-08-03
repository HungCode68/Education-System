package com.lms.education.module.reporting.service;

import com.lms.education.module.reporting.dto.ReportClassMetricsDto;

import java.time.LocalDate;
import java.util.List;

public interface ReportClassMetricsService {

    ReportClassMetricsDto generateOrUpdateClassMetrics(Long classId);

    List<ReportClassMetricsDto> generateOrUpdateAllClassesMetrics();

    ReportClassMetricsDto getClassMetrics(Long classId);

    List<ReportClassMetricsDto> getAllClassesMetrics();

    ReportClassMetricsDto getClassMetricsInRange(Long classId, LocalDate startDate, LocalDate endDate);

    List<ReportClassMetricsDto> getMyClassesMetrics();
}
