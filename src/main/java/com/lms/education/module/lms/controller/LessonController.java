package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.LessonDto;
import com.lms.education.module.lms.service.LessonService;
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
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasAuthority('LESSON_CREATE')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "lesson", description = "Tạo mới bài học")
    public ResponseEntity<Map<String, Object>> createLesson(@Valid @RequestBody LessonDto dto) {
        LessonDto created = lessonService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo bài học thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LESSON_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "lesson", description = "Cập nhật bài học")
    public ResponseEntity<Map<String, Object>> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonDto dto) {

        LessonDto updated = lessonService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật bài học thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LESSON_DELETE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "lesson", description = "Xóa bài học")
    public ResponseEntity<Map<String, String>> deleteLesson(@PathVariable Long id) {
        lessonService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa bài học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LESSON_VIEW')")
    public ResponseEntity<LessonDto> getLessonById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LESSON_VIEW')")
    public ResponseEntity<Page<LessonDto>> getAllLessons(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(lessonService.getAll(keyword, pageable));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('LESSON_VIEW')")
    public ResponseEntity<List<LessonDto>> getByClassId(@PathVariable Long classId) {
        return ResponseEntity.ok(lessonService.getByClassId(classId));
    }
}
