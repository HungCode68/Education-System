package com.lms.education.module.teaching_assignment.controller;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/teaching-assignments")
@RequiredArgsConstructor
@Slf4j
public class TeachingAssignmentController {

    private final TeachingAssignmentService assignmentService;

    // Phân công giáo viên (Tạo mới hoặc Thay thế)
    // POST /api/v1/teaching-assignments
    @PostMapping
    public ResponseEntity<TeachingAssignmentDto> assignTeacher(@Valid @RequestBody TeachingAssignmentDto dto) {
        log.info("Request phân công giáo viên: {}", dto.getTeacherId());
        return ResponseEntity.ok(assignmentService.assignTeacher(dto));
    }

    // Hủy phân công (Xóa)
    // DELETE /api/v1/teaching-assignments/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> unassignTeacher(@PathVariable String id) {
        assignmentService.unassignTeacher(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy phân công thành công"));
    }

    // Xem danh sách phân công của một Lớp (Theo học kỳ)
    // GET /api/v1/teaching-assignments/by-class/{classId}?semesterId=...
    @GetMapping("/by-class/{classId}")
    public ResponseEntity<List<TeachingAssignmentDto>> getAssignmentsByClass(
            @PathVariable String classId,
            @RequestParam String semesterId
    ) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByClass(classId, semesterId));
    }

    // Kiểm tra tải công việc của giáo viên (API hỗ trợ "Gác cổng")
    // GET /api/v1/teaching-assignments/workload?teacherId=...&semesterId=...
    // Giúp Frontend hiển thị: "Thầy A đang dạy 5 lớp"
    @GetMapping("/workload")
    public ResponseEntity<Map<String, Long>> checkWorkload(
            @RequestParam String teacherId,
            @RequestParam String semesterId
    ) {
        long count = assignmentService.countTeacherWorkload(teacherId, semesterId);
        return ResponseEntity.ok(Map.of("currentClasses", count));
    }
}