package com.lms.education.module.reporting.job;

import com.lms.education.module.reporting.service.ReportClassMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportClassMetricsJob {

    private final ReportClassMetricsService classMetricsService;

    /**
     * Tự động chạy vào lúc 00:05:00 mỗi đêm để tính toán và cập nhật KPI lớp học
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void executeDailyClassMetricsJob() {
        log.info("=== [REPORT CRON JOB] Bắt đầu tự động tính toán chỉ số KPI lớp học ===");
        try {
            classMetricsService.generateOrUpdateAllClassesMetrics();
            log.info("=== [REPORT CRON JOB] Hoàn tất tự động chốt KPI lớp học ===");
        } catch (Exception e) {
            log.error("=== [REPORT CRON JOB] Lỗi xảy ra khi chạy chốt KPI lớp học: {} ===", e.getMessage(), e);
        }
    }
}
