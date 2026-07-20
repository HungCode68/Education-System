package com.lms.education.module.teaching.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.teaching.dto.ScheduleAssignmentDto;
import com.lms.education.module.teaching.service.ScheduleAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/schedule-assignments")
@RequiredArgsConstructor
public class ScheduleAssignmentController {

    private final ScheduleAssignmentService scheduleAssignmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_CREATE')")
    @LogActivity(module = "TEACHING", action = "SCHEDULE_ASSIGN", targetType = "schedule_assignment", description = "Phân công giáo viên vào ca học")
    public ResponseEntity<Map<String, Object>> createAssignment(@Valid @RequestBody ScheduleAssignmentDto dto) {
        ScheduleAssignmentDto created = scheduleAssignmentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Phân công giáo viên vào ca học thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    @LogActivity(module = "TEACHING", action = "SCHEDULE_ASSIGN_UPDATE", targetType = "schedule_assignment", description = "Cập nhật phân công ca học")
    public ResponseEntity<Map<String, Object>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleAssignmentDto dto) {

        ScheduleAssignmentDto updated = scheduleAssignmentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật phân công ca học thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_DELETE')")
    @LogActivity(module = "TEACHING", action = "SCHEDULE_ASSIGN_DELETE", targetType = "schedule_assignment", description = "Hủy phân công ca học")
    public ResponseEntity<Map<String, String>> deleteAssignment(@PathVariable Long id) {
        scheduleAssignmentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Hủy phân công ca học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<ScheduleAssignmentDto> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(scheduleAssignmentService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<Page<ScheduleAssignmentDto>> getAllAssignments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(scheduleAssignmentService.getAll(keyword, pageable));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<List<ScheduleAssignmentDto>> getAssignmentsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(scheduleAssignmentService.getAssignmentsByClassId(classId));
    }

    @GetMapping("/schedule/{scheduleId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<List<ScheduleAssignmentDto>> getAssignmentsByScheduleId(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(scheduleAssignmentService.getAssignmentsByScheduleId(scheduleId));
    }
}
