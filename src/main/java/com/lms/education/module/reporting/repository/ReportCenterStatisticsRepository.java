package com.lms.education.module.reporting.repository;

import com.lms.education.module.reporting.entity.ReportCenterStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportCenterStatisticsRepository extends JpaRepository<ReportCenterStatistics, LocalDate> {

    List<ReportCenterStatistics> findByReportDateBetweenOrderByReportDateAsc(LocalDate startDate, LocalDate endDate);

    Optional<ReportCenterStatistics> findTopByOrderByReportDateDesc();
}
