package com.lms.education.module.user.controller;

import com.lms.education.module.user.dto.TeacherDto;
import com.lms.education.module.user.service.TeacherService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<TeacherDto> create(@Valid @RequestBody TeacherDto dto) {
        return new ResponseEntity<>(teacherService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<TeacherDto> update(@PathVariable String id, @Valid @RequestBody TeacherDto dto) {
        return ResponseEntity.ok(teacherService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        teacherService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<TeacherDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(teacherService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Page<TeacherDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.lms.education.module.user.entity.Teacher.Status status,
            @RequestParam(required = false) String departmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fullName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

        Page<TeacherDto> teachers = teacherService.getAll(keyword, status, departmentId, pageable);
        return ResponseEntity.ok(teachers);
    }


    // POST /api/teachers/{id}/create-account
    @PostMapping("/{id}/create-account")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Map<String, String>> createAccountForTeacher(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> request
    ) {
        String email = (request != null) ? request.get("email") : null;
        teacherService.createAccountForExistingTeacher(id, email);
        return ResponseEntity.ok(Map.of("message", "Đã cấp tài khoản thành công"));
    }

    // POST /api/teachers/create-accounts-batch
    // Body: { "teacherIds": ["id1", "id2", "id3"] }
    @PostMapping("/create-accounts-batch")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Map<String, Object>> createAccountsBatch(
            @RequestBody Map<String, java.util.List<String>> request) {

        java.util.List<String> teacherIds = request.get("teacherIds");

        if (teacherIds == null || teacherIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Danh sách ID giáo viên không được để trống!"));
        }

        Map<String, Object> result = teacherService.createAccountsBatch(teacherIds);
        return ResponseEntity.ok(result);
    }
}
