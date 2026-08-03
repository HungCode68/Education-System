package com.lms.education.module.reporting.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.reporting.dto.ReportClassMetricsDto;
import com.lms.education.module.reporting.service.ReportClassMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reporting/class-metrics")
@RequiredArgsConstructor
public class ReportClassMetricsController {

    private final ReportClassMetricsService classMetricsService;

    /**
     * Đồng bộ thủ công chỉ số KPI lớp học (Tất cả hoặc theo classId)
     */
    @PostMapping("/sync")
    @PreAuthorize("hasAnyAuthority('REPORT_CREATE', 'REPORT_UPDATE')")
    @LogActivity(module = "REPORTING", action = "SYNC", targetType = "report_class_metrics", description = "Đồng bộ chỉ số KPI lớp học")
    public ResponseEntity<Map<String, Object>> syncClassMetrics(
            @RequestParam(value = "classId", required = false) Long classId) {

        Map<String, Object> response = new HashMap<>();
        if (classId != null) {
            ReportClassMetricsDto dto = classMetricsService.generateOrUpdateClassMetrics(classId);
            response.put("message", "Đồng bộ chỉ số lớp học ID " + classId + " thành công!");
            response.put("data", dto);
        } else {
            List<ReportClassMetricsDto> list = classMetricsService.generateOrUpdateAllClassesMetrics();
            response.put("message", "Đồng bộ chỉ số KPI cho toàn bộ lớp học thành công!");
            response.put("data", list);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu chỉ số KPI tích lũy toàn khóa học của một lớp
     */
    @GetMapping("/{classId}")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportClassMetricsDto> getClassMetrics(@PathVariable("classId") Long classId) {
        return ResponseEntity.ok(classMetricsService.getClassMetrics(classId));
    }

    /**
     * Tra cứu chỉ số KPI lớp học trong một khoảng thời gian cụ thể (Đánh giá đổi GV / Đào tạo)
     */
    @GetMapping("/{classId}/custom-range")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportClassMetricsDto> getClassMetricsInRange(
            @PathVariable("classId") Long classId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(classMetricsService.getClassMetricsInRange(classId, startDate, endDate));
    }

    /**
     * Tra cứu chỉ số KPI lớp học trong 1 ngày cụ thể (Shortcut cho startDate = endDate)
     */
    @GetMapping("/{classId}/date/{date}")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<ReportClassMetricsDto> getClassMetricsByDate(
            @PathVariable("classId") Long classId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(classMetricsService.getClassMetricsInRange(classId, date, date));
    }

    /**
     * Lấy danh sách chỉ số KPI của tất cả các lớp học
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<List<ReportClassMetricsDto>> getAllClassMetrics() {
        return ResponseEntity.ok(classMetricsService.getAllClassesMetrics());
    }

    /**
     * Lấy danh sách chỉ số KPI các lớp học mà giáo viên đang được phân công giảng dạy (chính thức hoặc dạy thay)
     */
    @GetMapping("/my-classes")
    @PreAuthorize("hasAnyAuthority('REPORT_VIEW')")
    public ResponseEntity<List<ReportClassMetricsDto>> getMyClassMetrics() {
        return ResponseEntity.ok(classMetricsService.getMyClassesMetrics());
    }
}
