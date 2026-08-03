package com.lms.education.module.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDashboardDto {

    private TrainingOverviewDto centerOverview;
    private List<ReportClassMetricsDto> classMetricsList;
}
