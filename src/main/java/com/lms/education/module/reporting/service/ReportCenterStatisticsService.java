package com.lms.education.module.reporting.service;

import com.lms.education.module.reporting.dto.ReportCenterStatisticsDto;
import com.lms.education.module.reporting.dto.ReportSummaryDto;
import com.lms.education.module.reporting.dto.TrainingDashboardDto;

import java.time.LocalDate;
import java.util.List;

public interface ReportCenterStatisticsService {

    ReportCenterStatisticsDto generateOrUpdateDailyReport(LocalDate date);

    ReportCenterStatisticsDto saveCustomReportSnapshot(ReportCenterStatisticsDto dto);

    ReportCenterStatisticsDto getReportByDate(LocalDate date);

    List<ReportCenterStatisticsDto> getReportsBetween(LocalDate startDate, LocalDate endDate);

    ReportSummaryDto getSummaryReportBetween(LocalDate startDate, LocalDate endDate);

    ReportCenterStatisticsDto getLatestReport();

    TrainingDashboardDto getTrainingDashboard(LocalDate date);
}
