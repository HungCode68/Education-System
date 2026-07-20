package com.lms.education.module.enrollment.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.enrollment.dto.BulkEnrollmentDto;
import com.lms.education.module.enrollment.dto.EnrollmentDto;
import com.lms.education.module.enrollment.service.EnrollmentService;
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
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('ENROLLMENT_CREATE')")
    @LogActivity(module = "ENROLLMENT", action = "CREATE", targetType = "enrollment", description = "Đăng ký học viên vào lớp học")
    public ResponseEntity<Map<String, Object>> createEnrollment(@Valid @RequestBody EnrollmentDto dto) {
        EnrollmentDto created = enrollmentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký học viên vào lớp thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ENROLLMENT_UPDATE')")
    @LogActivity(module = "ENROLLMENT", action = "UPDATE", targetType = "enrollment", description = "Cập nhật thông tin đăng ký học viên")
    public ResponseEntity<Map<String, Object>> updateEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentDto dto) {

        EnrollmentDto updated = enrollmentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật đăng ký thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ENROLLMENT_DELETE')")
    @LogActivity(module = "ENROLLMENT", action = "DELETE", targetType = "enrollment", description = "Hủy đăng ký học viên khỏi lớp")
    public ResponseEntity<Map<String, String>> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa đăng ký học viên thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<EnrollmentDto> getEnrollmentById(@PathVariable Long id) {
        return ResponseEntity.ok(enrollmentService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<Page<EnrollmentDto>> getAllEnrollments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(enrollmentService.getAll(keyword, pageable));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<List<EnrollmentDto>> getByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(enrollmentService.getByStudentId(studentId));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<List<EnrollmentDto>> getByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(enrollmentService.getByClassId(classId));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('ENROLLMENT_CREATE')")
    @LogActivity(module = "ENROLLMENT", action = "BULK_CREATE", targetType = "enrollment", description = "Đăng ký nhiều học viên vào lớp học")
    public ResponseEntity<Map<String, Object>> createBulkEnrollment(@Valid @RequestBody BulkEnrollmentDto dto) {
        Map<String, Object> report = enrollmentService.enrollBulk(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đăng ký học viên hàng loạt hoàn tất!");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }
}
