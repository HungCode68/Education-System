package com.lms.education.module.teaching.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.teaching.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching.service.TeachingAssignmentService;
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
@RequestMapping("/api/v1/teaching-assignments")
@RequiredArgsConstructor
public class TeachingAssignmentController {

    private final TeachingAssignmentService teachingAssignmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_CREATE')")
    @LogActivity(module = "TEACHING", action = "ASSIGN", targetType = "teaching_assignment", description = "Tạo phân công giảng dạy mới")
    public ResponseEntity<Map<String, Object>> createAssignment(@Valid @RequestBody TeachingAssignmentDto dto) {
        TeachingAssignmentDto created = teachingAssignmentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Phân công giảng dạy thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    @LogActivity(module = "TEACHING", action = "UPDATE", targetType = "teaching_assignment", description = "Cập nhật phân công giảng dạy")
    public ResponseEntity<Map<String, Object>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody TeachingAssignmentDto dto) {

        TeachingAssignmentDto updated = teachingAssignmentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật phân công giảng dạy thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_DELETE')")
    @LogActivity(module = "TEACHING", action = "DELETE", targetType = "teaching_assignment", description = "Xóa phân công giảng dạy")
    public ResponseEntity<Map<String, String>> deleteAssignment(@PathVariable Long id) {
        teachingAssignmentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa phân công giảng dạy thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<TeachingAssignmentDto> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(teachingAssignmentService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<Page<TeachingAssignmentDto>> getAllAssignments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(teachingAssignmentService.getAllAssignments(keyword, pageable));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<List<TeachingAssignmentDto>> getAssignmentsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(teachingAssignmentService.getAssignmentsByClassId(classId));
    }
}
