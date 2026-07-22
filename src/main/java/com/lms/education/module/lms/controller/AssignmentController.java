package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.AssignmentDto;
import com.lms.education.module.lms.service.AssignmentService;
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
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_CREATE')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "assignment", description = "Tạo mới bài tập cho bài học")
    public ResponseEntity<Map<String, Object>> createAssignment(@Valid @RequestBody AssignmentDto dto) {
        AssignmentDto created = assignmentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo mới bài tập thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "assignment", description = "Cập nhật thông tin bài tập")
    public ResponseEntity<Map<String, Object>> updateAssignment(@PathVariable Long id, @RequestBody AssignmentDto dto) {
        AssignmentDto updated = assignmentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật bài tập thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_DELETE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "assignment", description = "Xóa bài tập")
    public ResponseEntity<Map<String, String>> deleteAssignment(@PathVariable Long id) {
        assignmentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa bài tập thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_VIEW')")
    public ResponseEntity<AssignmentDto> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getById(id));
    }

    @GetMapping("/lesson/{lessonId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_VIEW')")
    public ResponseEntity<List<AssignmentDto>> getAssignmentsByLessonId(@PathVariable Long lessonId) {
        return ResponseEntity.ok(assignmentService.getByLessonId(lessonId));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_VIEW')")
    public ResponseEntity<List<AssignmentDto>> getAssignmentsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(assignmentService.getByClassId(classId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_VIEW')")
    public ResponseEntity<Page<AssignmentDto>> getAllAssignments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(assignmentService.getAll(keyword, pageable));
    }
}
