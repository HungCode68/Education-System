package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')") // đổi lại thành quyền user_create của admin hệ thống (có toàn quyền)
    public ResponseEntity<StudentDto> create(@Valid @RequestBody StudentDto dto) {
        return new ResponseEntity<>(studentService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    public ResponseEntity<StudentDto> update(@PathVariable String id, @Valid @RequestBody StudentDto dto) {
        return ResponseEntity.ok(studentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        studentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasAuthority('CLASS_VIEW')")
    public ResponseEntity<StudentDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasAuthority('CLASS_VIEW')")
    public ResponseEntity<Page<StudentDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Student.Status status,
            @RequestParam(required = false) Integer admissionYear,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        // Cấu hình sắp xếp (Sort)
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // Cấu hình phân trang (Pageable)
        Pageable pageable = PageRequest.of(page, size, sort);

        // Gọi Service
        Page<StudentDto> students = studentService.getAll(keyword, status, admissionYear, pageable);

        return ResponseEntity.ok(students);
    }

    // POST /api/students/{id}/create-account
    // Body: { "email": "hs002@school.edu.vn" }
    @PostMapping("/{id}/create-account")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Map<String, String>> createAccountForStudent(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String email = (request != null) ? request.get("email") : null;
        studentService.createAccountForExistingStudent(id, email);
        return ResponseEntity.ok(Map.of("message", "Đã cấp tài khoản thành công"));
    }

    // POST /api/students/create-accounts-batch
    // Body: { "studentIds": ["id1", "id2", "id3"] }
    @PostMapping("/create-accounts-batch")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Map<String, Object>> createAccountsBatch(
            @RequestBody Map<String, java.util.List<String>> request) {

        java.util.List<String> studentIds = request.get("studentIds");

        if (studentIds == null || studentIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Danh sách ID học sinh không được để trống!"));
        }

        Map<String, Object> result = studentService.createAccountsBatch(studentIds);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/my-profile")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<StudentDto> getMyProfile(Principal principal) {
        // Lấy userId từ Token
        com.lms.education.security.UserPrincipal userPrincipal =
                (com.lms.education.security.UserPrincipal) ((org.springframework.security.core.Authentication) principal).getPrincipal();

        String userId = userPrincipal.getId();
        return ResponseEntity.ok(studentService.getMyProfile(userId));
    }
}
