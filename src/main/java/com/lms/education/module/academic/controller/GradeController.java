package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.GradeDto;
import com.lms.education.module.academic.service.GradeService;
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
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
@Slf4j
public class GradeController {

    private final GradeService gradeService;

    // Tìm kiếm và Phân trang (Admin quản lý)
    // GET /api/v1/grades?keyword=khoi&isActive=true&page=1&size=10
    @GetMapping
    public ResponseEntity<PageResponse<GradeDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("REST request to search Grades. Keyword: {}, Active: {}", keyword, isActive);
        return ResponseEntity.ok(gradeService.search(keyword, isActive, page, size));
    }

    // Lấy danh sách Khối đang hoạt động (Dùng cho Dropdown)
    // GET /api/v1/grades/active-list
    @GetMapping("/active-list")
    public ResponseEntity<List<GradeDto>> getAllActive() {
        log.info("REST request to get all Active Grades for dropdown");
        return ResponseEntity.ok(gradeService.getAllActive());
    }

    // Lấy chi tiết theo ID
    // GET /api/v1/grades/{id}
    @GetMapping("/{id}")
    public ResponseEntity<GradeDto> getById(@PathVariable String id) {
        log.info("REST request to get Grade ID: {}", id);
        return ResponseEntity.ok(gradeService.getById(id));
    }

    // Tạo mới
    // POST /api/v1/grades
    @PostMapping
    public ResponseEntity<GradeDto> create(@Valid @RequestBody GradeDto dto) {
        log.info("REST request to create Grade: {}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(dto));
    }

    // Cập nhật
    // PUT /api/v1/grades/{id}
    @PutMapping("/{id}")
    public ResponseEntity<GradeDto> update(
            @PathVariable String id,
            @Valid @RequestBody GradeDto dto
    ) {
        log.info("REST request to update Grade ID: {}", id);
        return ResponseEntity.ok(gradeService.update(id, dto));
    }

    // Xóa
    // DELETE /api/v1/grades/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        log.info("REST request to delete Grade ID: {}", id);
        gradeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa khối thành công!"));
    }
}
