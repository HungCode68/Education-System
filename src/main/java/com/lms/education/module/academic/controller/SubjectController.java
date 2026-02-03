package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.SubjectDto;
import com.lms.education.module.academic.service.SubjectService;
import com.lms.education.utils.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
@Slf4j
public class SubjectController {

    private final SubjectService subjectService;

    // Tìm kiếm và Phân trang
    // GET /api/v1/subjects?keyword=toan&isActive=true&page=1&size=10
    @GetMapping
    public ResponseEntity<PageResponse<SubjectDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("REST request to search Subjects. Keyword: {}, Active: {}", keyword, isActive);
        return ResponseEntity.ok(subjectService.search(keyword, isActive, page, size));
    }

    // Lấy danh sách môn đang hoạt động (Dùng cho Dropdown chọn môn học)
    // GET /api/v1/subjects/active-list
    @GetMapping("/active-list")
    public ResponseEntity<List<SubjectDto>> getAllActive() {
        log.info("REST request to get all Active Subjects for dropdown");
        return ResponseEntity.ok(subjectService.getAllActive());
    }

    // Lấy chi tiết theo ID
    // GET /api/v1/subjects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SubjectDto> getById(@PathVariable String id) {
        log.info("REST request to get Subject ID: {}", id);
        return ResponseEntity.ok(subjectService.getById(id));
    }

    // Tạo mới
    // POST /api/v1/subjects
    @PostMapping
    public ResponseEntity<SubjectDto> create(@Valid @RequestBody SubjectDto dto) {
        log.info("REST request to create Subject: {}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(dto));
    }

    // Cập nhật
    // PUT /api/v1/subjects/{id}
    @PutMapping("/{id}")
    public ResponseEntity<SubjectDto> update(
            @PathVariable String id,
            @Valid @RequestBody SubjectDto dto
    ) {
        log.info("REST request to update Subject ID: {}", id);
        return ResponseEntity.ok(subjectService.update(id, dto));
    }

    // Xóa
    // DELETE /api/v1/subjects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        log.info("REST request to delete Subject ID: {}", id);
        subjectService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa môn học thành công!"));
    }
}