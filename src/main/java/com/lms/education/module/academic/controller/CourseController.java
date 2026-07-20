package com.lms.education.module.academic.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.academic.dto.CourseDto;
import com.lms.education.module.academic.service.CourseService;
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
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAuthority('COURSES_CREATE')")
    @LogActivity(module = "COURSE", action = "CREATE", targetType = "course", description = "Tạo mới khóa học")
    public ResponseEntity<Map<String, Object>> createCourse(@Valid @RequestBody CourseDto dto) {
        CourseDto createdCourse = courseService.create(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo khóa học thành công!");
        response.put("data", createdCourse);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSES_UPDATE')")
    @LogActivity(module = "COURSE", action = "UPDATE", targetType = "course", description = "Cập nhật thông tin khóa học")
    public ResponseEntity<Map<String, Object>> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseDto dto) {

        CourseDto updatedCourse = courseService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật thông tin khóa học thành công!");
        response.put("data", updatedCourse);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSES_DELETE')")
    @LogActivity(module = "COURSE", action = "DELETE", targetType = "course", description = "Xóa khóa học")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        courseService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa khóa học thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COURSES_VIEW')")
    public ResponseEntity<CourseDto> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('COURSES_VIEW')")
    public ResponseEntity<CourseDto> getCourseByCode(@PathVariable String code) {
        return ResponseEntity.ok(courseService.getByCode(code));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('COURSES_VIEW')")
    public ResponseEntity<Page<CourseDto>> getAllCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(courseService.getAllCourses(keyword, pageable));
    }
}