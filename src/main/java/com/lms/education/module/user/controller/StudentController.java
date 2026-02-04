package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
            @PageableDefault(sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(studentService.getAll(pageable));
    }
}
