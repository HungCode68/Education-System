package com.lms.education.module.academic.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.academic.dto.ClassesDto;
import com.lms.education.module.academic.service.ClassesService;
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
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
public class ClassesController {

    private final ClassesService classesService;

    @PostMapping
    @PreAuthorize("hasAuthority('CLASS_CREATE')")
    @LogActivity(module = "CLASS", action = "CREATE", targetType = "classes", description = "Tạo mới lớp học")
    public ResponseEntity<Map<String, Object>> createClass(@Valid @RequestBody ClassesDto dto) {
        ClassesDto createdClass = classesService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo lớp học thành công!");
        response.put("data", createdClass);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_UPDATE')")
    @LogActivity(module = "CLASS", action = "UPDATE", targetType = "classes", description = "Cập nhật thông tin lớp học")
    public ResponseEntity<Map<String, Object>> updateClass(
            @PathVariable Long id,
            @Valid @RequestBody ClassesDto dto) {

        ClassesDto updatedClass = classesService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin lớp học thành công!");
        response.put("data", updatedClass);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_DELETE')")
    @LogActivity(module = "CLASS", action = "DELETE", targetType = "classes", description = "Xóa lớp học")
    public ResponseEntity<Map<String, String>> deleteClass(@PathVariable Long id) {
        classesService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa lớp học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public ResponseEntity<ClassesDto> getClassById(@PathVariable Long id) {
        return ResponseEntity.ok(classesService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLASS_VIEW')")
    public ResponseEntity<Page<ClassesDto>> getAllClasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(classesService.getAllClasses(keyword, pageable));
    }

    @GetMapping("/my-classes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<java.util.List<ClassesDto>> getMyClasses() {
        return ResponseEntity.ok(classesService.getMyClasses());
    }
}
