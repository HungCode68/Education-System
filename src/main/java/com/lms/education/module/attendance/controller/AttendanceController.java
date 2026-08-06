package com.lms.education.module.attendance.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.attendance.dto.AttendanceDto;
import com.lms.education.module.attendance.service.AttendanceService;
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
@RequestMapping("/api/v1/attendances")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/schedule/{scheduleId}/date/{attendanceDate}")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_VIEW', 'CLASS_SCHEDULE_VIEW', 'TEACHING_SCHEDULE_VIEW', 'CLASS_VIEW', 'LMS_CLASS_VIEW') or isAuthenticated()")
    public ResponseEntity<List<AttendanceDto>> getAttendanceSheetByScheduleAndDate(
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate) {
        return ResponseEntity.ok(attendanceService.getAttendanceSheetByScheduleAndDate(scheduleId, attendanceDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_CREATE', 'ATTENDANCE_UPDATE', 'TEACHING_SCHEDULE_VIEW', 'CLASS_VIEW', 'LMS_CLASS_VIEW') or isAuthenticated()")
    @LogActivity(module = "ATTENDANCE", action = "CREATE", targetType = "attendance", description = "Điểm danh cho từng học viên")
    public ResponseEntity<Map<String, Object>> markAttendance(@Valid @RequestBody AttendanceDto dto) {
        AttendanceDto saved = attendanceService.markAttendance(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lưu trạng thái điểm danh thành công!");
        response.put("data", saved);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/schedule/{scheduleId}/date/{attendanceDate}/batch")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_CREATE', 'ATTENDANCE_UPDATE', 'TEACHING_SCHEDULE_VIEW', 'CLASS_VIEW', 'LMS_CLASS_VIEW') or isAuthenticated()")
    @LogActivity(module = "ATTENDANCE", action = "UPDATE", targetType = "attendance", description = "Điểm danh hàng loạt cho toàn bộ danh sách lớp học")
    public ResponseEntity<Map<String, Object>> batchMarkAttendance(
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate attendanceDate,
            @RequestBody List<AttendanceDto> dtos) {

        List<AttendanceDto> sheet = attendanceService.batchMarkAttendance(scheduleId, attendanceDate, dtos);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lưu bảng điểm danh lớp học thành công!");
        response.put("data", sheet);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_VIEW', 'STUDENT_VIEW')")
    public ResponseEntity<List<AttendanceDto>> getAttendanceByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(attendanceService.getAttendanceByStudent(studentId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ATTENDANCE_DELETE')")
    @LogActivity(module = "ATTENDANCE", action = "DELETE", targetType = "attendance", description = "Xóa bản ghi điểm danh")
    public ResponseEntity<Map<String, Object>> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa bản ghi điểm danh thành công!");

        return ResponseEntity.ok(response);
    }
}
