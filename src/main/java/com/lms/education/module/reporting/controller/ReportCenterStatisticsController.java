package com.lms.education.module.reporting.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.reporting.dto.ReportCenterStatisticsDto;
import com.lms.education.module.reporting.dto.ReportSummaryDto;
import com.lms.education.module.reporting.dto.TrainingDashboardDto;
import com.lms.education.module.reporting.service.ReportCenterStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporting/statistics")
@RequiredArgsConstructor
public class ReportCenterStatisticsController {

    private final ReportCenterStatisticsService reportService;

    /**
     * Đồng bộ ngay / Chốt số liệu thống kê báo cáo theo ngày (Mặc định ngày hiện tại)
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyAuthority('REPORT_CREATE', 'REPORT_UPDATE')")
    @LogActivity(module = "REPORTING", action = "SYNC", targetType = "report_center_statistics", description = "Đồng bộ ngay số liệu thống kê trung tâm")
    public ResponseEntity<Map<String, Object>> syncDailyReport(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        ReportCenterStatisticsDto synced = reportService.generateOrUpdateDailyReport(date);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đồng bộ/Chốt số liệu thống kê trung tâm thành công!");
        response.put("data", synced);

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật / Nhập thủ công số liệu báo cáo
     */
    @PostMapping("/snapshot")
    @PreAuthorize("hasAnyAuthority('REPORT_CREATE', 'REPORT_UPDATE')")
    @LogActivity(module = "REPORTING", action = "CREATE", targetType = "report_center_statistics", description = "Lưu bản ghi chốt số liệu báo cáo thủ công")
    public ResponseEntity<Map<String, Object>> saveCustomSnapshot(@Valid @RequestBody ReportCenterStatisticsDto dto) {
        ReportCenterStatisticsDto saved = reportService.saveCustomReportSnapshot(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lưu số liệu báo cáo trung tâm thành công!");
        response.put("data", saved);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Xem số liệu thống kê báo cáo theo ngày cụ thể
     */
    @GetMapping("/{date}")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportCenterStatisticsDto> getReportByDate(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getReportByDate(date));
    }

    /**
     * Xem số liệu thống kê trong một khoảng thời gian (phục vụ biểu đồ xu hướng)
     */
    @GetMapping("/range")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<List<ReportCenterStatisticsDto>> getReportsBetween(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getReportsBetween(startDate, endDate));
    }

    /**
     * Xem thẻ thống kê tổng hợp (Summary KPI) trong một khoảng thời gian
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportSummaryDto> getSummaryReportBetween(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(reportService.getSummaryReportBetween(startDate, endDate));
    }

    /**
     * Lấy số liệu thống kê gần nhất trong hệ thống
     */
    @GetMapping("/latest")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportCenterStatisticsDto> getLatestReport() {
        return ResponseEntity.ok(reportService.getLatestReport());
    }

    /**
     * Báo cáo tổng hợp dành riêng cho Bộ phận Đào tạo (Tổng quan học viên, giáo viên, khóa học, lớp học + Chi tiết từng lớp)
     */
    @GetMapping("/training-dashboard")
    @PreAuthorize("hasAnyAuthority('TRAINING_VIEW')")
    public ResponseEntity<TrainingDashboardDto> getTrainingDashboard(
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(reportService.getTrainingDashboard(date));
    }
}
