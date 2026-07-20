package com.lms.education.module.teaching.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching.service.TeachingSubstitutionService;
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
@RequestMapping("/api/v1/teaching-substitutions")
@RequiredArgsConstructor
public class TeachingSubstitutionController {

    private final TeachingSubstitutionService teachingSubstitutionService;

    @PostMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_CREATE')")
    @LogActivity(module = "TEACHING", action = "SUBSTITUTE", targetType = "teaching_substitution", description = "Tạo phân công dạy thay mới")
    public ResponseEntity<Map<String, Object>> createSubstitution(@Valid @RequestBody TeachingSubstitutionDto dto) {
        TeachingSubstitutionDto created = teachingSubstitutionService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Phân công dạy thay thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    @LogActivity(module = "TEACHING", action = "SUBSTITUTE_UPDATE", targetType = "teaching_substitution", description = "Cập nhật phân công dạy thay")
    public ResponseEntity<Map<String, Object>> updateSubstitution(
            @PathVariable Long id,
            @Valid @RequestBody TeachingSubstitutionDto dto) {

        TeachingSubstitutionDto updated = teachingSubstitutionService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật phân công dạy thay thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_DELETE')")
    @LogActivity(module = "TEACHING", action = "SUBSTITUTE_DELETE", targetType = "teaching_substitution", description = "Xóa phân công dạy thay")
    public ResponseEntity<Map<String, String>> deleteSubstitution(@PathVariable Long id) {
        teachingSubstitutionService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa phân công dạy thay thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<TeachingSubstitutionDto> getSubstitutionById(@PathVariable Long id) {
        return ResponseEntity.ok(teachingSubstitutionService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<Page<TeachingSubstitutionDto>> getAllSubstitutions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(teachingSubstitutionService.getAll(keyword, pageable));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<List<TeachingSubstitutionDto>> getSubstitutionsByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(teachingSubstitutionService.getSubstitutionsByClassId(classId));
    }
}
