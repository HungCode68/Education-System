package com.lms.education.module.teaching_assignment.controller;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentService;
import com.lms.education.utils.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAuthority('TEACHING_ASSIGN')")
    public ResponseEntity<TeachingAssignmentDto> assignTeacher(
            @Valid @RequestBody TeachingAssignmentDto dto,
            java.security.Principal principal) {

        // Lấy ID của Tổ trưởng/Admin đang thao tác
        com.lms.education.security.UserPrincipal userPrincipal = (com.lms.education.security.UserPrincipal) ((org.springframework.security.core.Authentication) principal).getPrincipal();
        String currentUserId = userPrincipal.getId();

        log.info("Request phân công giáo viên: {} bởi user: {}", dto.getTeacherId(), currentUserId);

        // Truyền thêm currentUserId vào Service
        return ResponseEntity.ok(assignmentService.assignTeacher(dto, currentUserId));
    }

    // Hủy phân công (Xóa)
    // DELETE /api/v1/teaching-assignments/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TEACHING_ASSIGN')")
    public ResponseEntity<Map<String, String>> unassignTeacher(@PathVariable String id) {
        assignmentService.unassignTeacher(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy phân công thành công"));
    }

    // Xem danh sách phân công của một Lớp (Theo học kỳ)
    // GET /api/v1/teaching-assignments/by-class/{classId}?semesterId=...
    @GetMapping("/by-class/{classId}")
    @PreAuthorize("hasAuthority('TEACHING_ASSIGN') or hasAuthority('ONLINE_CLASS_VIEW')")
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
    @PreAuthorize("hasAuthority('TEACHING_ASSIGN')")
    public ResponseEntity<Map<String, Long>> checkWorkload(
            @RequestParam String teacherId,
            @RequestParam String semesterId
    ) {
        long count = assignmentService.countTeacherWorkload(teacherId, semesterId);
        return ResponseEntity.ok(Map.of("currentClasses", count));
    }

    // Xem danh sách phân công của Tổ bộ môn
    // GET /api/v1/teaching-assignments/department
    @GetMapping("/department")
    @PreAuthorize("hasAnyRole('TEACHER_HEAD_DEPARTMENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<PageResponse<TeachingAssignmentDto>> getByDepartment(
            @RequestParam String departmentId,
            @RequestParam(required = false) String schoolYearId,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String physicalClassId,
            @RequestParam(required = false) String teacherId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByDepartment(
                departmentId, schoolYearId, semesterId,physicalClassId, teacherId, page, size));
    }
}