package com.lms.education.module.academic.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.academic.dto.ClassScheduleDto;
import com.lms.education.module.academic.dto.TimetableEntryDto;
import com.lms.education.module.academic.service.ClassScheduleService;
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
@RequestMapping("/api/v1/class-schedules")
@RequiredArgsConstructor
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SCHEDULE_CREATE')")
    @LogActivity(module = "SCHEDULE", action = "CREATE", targetType = "schedule", description = "Tạo mới lịch học")
    public ResponseEntity<Map<String, Object>> createSchedule(@Valid @RequestBody ClassScheduleDto dto) {
        ClassScheduleDto created = classScheduleService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo lịch học thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_UPDATE')")
    @LogActivity(module = "SCHEDULE", action = "UPDATE", targetType = "schedule", description = "Cập nhật thông tin lịch học")
    public ResponseEntity<Map<String, Object>> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ClassScheduleDto dto) {

        ClassScheduleDto updated = classScheduleService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin lịch học thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_DELETE')")
    @LogActivity(module = "SCHEDULE", action = "DELETE", targetType = "schedule", description = "Xóa lịch học")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Long id) {
        classScheduleService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa lịch học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ResponseEntity<ClassScheduleDto> getScheduleById(@PathVariable Long id) {
        return ResponseEntity.ok(classScheduleService.getById(id));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ResponseEntity<List<ClassScheduleDto>> getSchedulesByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(classScheduleService.getSchedulesByClassId(classId));
    }

    @GetMapping("/student/{studentId}/timetable")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ResponseEntity<List<TimetableEntryDto>> getStudentTimetable(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(classScheduleService.getStudentTimetable(studentId, startDate, endDate));
    }

    @GetMapping("/teacher/{teacherId}/timetable")
    @PreAuthorize("hasAuthority('SCHEDULE_VIEW')")
    public ResponseEntity<List<TimetableEntryDto>> getTeacherTimetable(
            @PathVariable Long teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(classScheduleService.getTeacherTimetable(teacherId, startDate, endDate));
    }
}
