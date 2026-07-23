package com.lms.education.module.user.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.DepartmentDto;
import com.lms.education.module.user.service.DepartmentService;
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
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_CREATE')")
    @LogActivity(module = "DEPARTMENT", action = "CREATE", targetType = "department", description = "Tạo mới phòng ban/khoa hệ thống")
    public ResponseEntity<Map<String, Object>> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        DepartmentDto createdDepartment = departmentService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo phòng ban/khoa thành công!");
        response.put("data", createdDepartment);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_UPDATE')")
    @LogActivity(module = "DEPARTMENT", action = "UPDATE", targetType = "department", description = "Cập nhật thông tin phòng ban/khoa")
    public ResponseEntity<Map<String, Object>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentDto dto) {

        DepartmentDto updatedDepartment = departmentService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin phòng ban/khoa thành công!");
        response.put("data", updatedDepartment);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_DELETE')")
    @LogActivity(module = "DEPARTMENT", action = "DELETE", targetType = "department", description = "Xóa phòng ban/khoa khỏi hệ thống")
    public ResponseEntity<Map<String, String>> deleteDepartment(@PathVariable Long id) {
        departmentService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa phòng ban/khoa thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<DepartmentDto> getDepartmentByCode(@PathVariable String code) {
        return ResponseEntity.ok(departmentService.getByCode(code));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<Page<DepartmentDto>> getAllDepartments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(departmentService.getAllDepartments(keyword, pageable));
    }
}