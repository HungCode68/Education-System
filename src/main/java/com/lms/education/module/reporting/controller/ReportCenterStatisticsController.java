package com.lms.education.module.reporting.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.reporting.dto.ReportCenterStatisticsDto;
import com.lms.education.module.reporting.dto.ReportSummaryDto;
import com.lms.education.module.reporting.dto.TrainingDashboardDto;
import com.lms.education.module.reporting.service.ReportCenterStatisticsService;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final ClassesRepository classesRepository;

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
        ReportCenterStatisticsDto centerReport = reportService.getLatestReport();
        return ResponseEntity.ok(centerReport);
    }

    @PostMapping("/details/students")
    public ResponseEntity<List<Map<String, Object>>> getStudentDetails(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());
        List<Map<String, Object>> result = studentRepository.findAllById(ids).stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("code", s.getStudentCode());
            map.put("name", s.getFullName());
            map.put("phone", s.getPhone());
            map.put("dob", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : null);
            map.put("address", s.getAddress());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/details/staffs")
    public ResponseEntity<List<Map<String, Object>>> getStaffDetails(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());
        List<Map<String, Object>> result = staffRepository.findAllById(ids).stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("code", s.getStaffCode());
            map.put("name", s.getFullName());
            map.put("phone", s.getPhone());
            map.put("dob", s.getDateOfBirth() != null ? s.getDateOfBirth().toString() : null);
            map.put("address", s.getAddress());
            map.put("role", s.getStaffType());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/details/classes")
    public ResponseEntity<List<Map<String, Object>>> getClassDetails(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) return ResponseEntity.ok(List.of());
        List<Map<String, Object>> result = classesRepository.findAllById(ids).stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("code", c.getCode());
            map.put("name", c.getName());
            map.put("status", c.getStatus());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
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
