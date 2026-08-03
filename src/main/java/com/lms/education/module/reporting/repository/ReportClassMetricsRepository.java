package com.lms.education.module.reporting.repository;

import com.lms.education.module.reporting.entity.ReportClassMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportClassMetricsRepository extends JpaRepository<ReportClassMetrics, Long> {
}
