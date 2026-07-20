package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.StudentProvisionDto;
import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.service.StudentService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_CREATE')")
    @LogActivity(module = "STUDENT", action = "CREATE", targetType = "student", description = "Tạo mới hồ sơ học viên")
    public ResponseEntity<Map<String, Object>> createStudent(@Valid @RequestBody StudentDto dto) {
        StudentDto createdStudent = studentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo hồ sơ học viên thành công!");
        response.put("data", createdStudent);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_UPDATE')")
    @LogActivity(module = "STUDENT", action = "UPDATE", targetType = "student", description = "Cập nhật thông tin học viên")
    public ResponseEntity<Map<String, Object>> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentDto dto) {

        StudentDto updatedStudent = studentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin học viên thành công!");
        response.put("data", updatedStudent);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_DELETE')")
    @LogActivity(module = "STUDENT", action = "DELETE", targetType = "student", description = "Xóa hồ sơ học viên")
    public ResponseEntity<Map<String, String>> deleteStudent(@PathVariable Long id) {
        studentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa hồ sơ học viên thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<StudentDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getById(id));
    }

    @GetMapping("/code/{studentCode}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<StudentDto> getStudentByCode(@PathVariable String studentCode) {
        return ResponseEntity.ok(studentService.getByStudentCode(studentCode));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STUDENT_VIEW')")
    public ResponseEntity<Page<StudentDto>> getAllStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(studentService.getAllStudents(keyword, pageable));
    }

    @PostMapping("/provision-accounts")
    @PreAuthorize("hasAuthority('STUDENT_PROVISION')")
    @LogActivity(module = "STUDENT", action = "PROVISION", targetType = "user", description = "Cấp tài khoản hàng loạt cho học viên")
    public ResponseEntity<Map<String, Object>> provisionAccounts(@Valid @RequestBody StudentProvisionDto dto) {

        Map<String, Object> report = studentService.provisionAccounts(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tiến trình cấp tài khoản học viên đã hoàn tất!");
        response.put("data", report);

        return ResponseEntity.ok(response);
    }
}