package com.lms.education.module.reporting.job;

import com.lms.education.module.reporting.service.ReportCenterStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportCenterStatisticsJob {

    private final ReportCenterStatisticsService reportService;

    /**
     * Tự động chạy vào lúc 23:59 mỗi ngày để chốt số liệu thống kê báo cáo ngày hôm đó
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void runDailyReportSnapshot() {
        LocalDate today = LocalDate.now();
        log.info("=== Bắt đầu tiến hành Job tự động chốt số liệu thống kê trung tâm ngày: {} ===", today);
        try {
            reportService.generateOrUpdateDailyReport(today);
            log.info("=== Hoàn tất Job tự động chốt số liệu thống kê trung tâm ngày: {} ===", today);
        } catch (Exception e) {
            log.error("Lỗi khi tự động chốt số liệu báo cáo ngày: {}", today, e);
        }
    }
}
