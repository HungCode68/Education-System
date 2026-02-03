package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.GradeSubjectDto;
import com.lms.education.module.academic.service.GradeSubjectService;
import com.lms.education.utils.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grade-subjects")
@RequiredArgsConstructor
@Slf4j
public class GradeSubjectController {

    private final GradeSubjectService gradeSubjectService;

    // Tìm kiếm và Phân trang
    // GET /api/v1/grade-subjects?gradeId=...&keyword=toan&page=1&size=10
    @GetMapping
    public ResponseEntity<PageResponse<GradeSubjectDto>> search(
            @RequestParam(required = false) String gradeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("REST request to search GradeSubjects. GradeID: {}, Keyword: {}", gradeId, keyword);
        return ResponseEntity.ok(gradeSubjectService.search(gradeId, keyword, page, size));
    }

    // Lấy danh sách môn học theo Khối
    // GET /api/v1/grade-subjects/by-grade/{gradeId}?onlyLmsEnabled=true
    // - Dùng cho User (onlyLmsEnabled=true): Học sinh xem danh sách môn học của mình.
    @GetMapping("/by-grade/{gradeId}")
    public ResponseEntity<List<GradeSubjectDto>> getByGradeId(
            @PathVariable String gradeId,
            @RequestParam(defaultValue = "false") boolean onlyLmsEnabled
    ) {
        log.info("REST request to get Subjects by GradeID: {}, OnlyLMS: {}", gradeId, onlyLmsEnabled);
        return ResponseEntity.ok(gradeSubjectService.getByGradeId(gradeId, onlyLmsEnabled));
    }

    // Lấy chi tiết theo ID
    // GET /api/v1/grade-subjects/{id}
    @GetMapping("/{id}")
    public ResponseEntity<GradeSubjectDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(gradeSubjectService.getById(id));
    }

    // Tạo mới (Gán môn vào khối)
    // POST /api/v1/grade-subjects
    @PostMapping
    public ResponseEntity<GradeSubjectDto> create(@Valid @RequestBody GradeSubjectDto dto) {
        log.info("REST request to create GradeSubject mapping: Grade {} - Subject {}", dto.getGradeId(), dto.getSubjectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeSubjectService.create(dto));
    }

    // Cập nhật (Sửa cấu hình: thứ tự, bật/tắt LMS)
    // PUT /api/v1/grade-subjects/{id}
    @PutMapping("/{id}")
    public ResponseEntity<GradeSubjectDto> update(
            @PathVariable String id,
            @Valid @RequestBody GradeSubjectDto dto
    ) {
        log.info("REST request to update GradeSubject ID: {}", id);
        return ResponseEntity.ok(gradeSubjectService.update(id, dto));
    }

    // Xóa (Gỡ môn khỏi khối)
    // DELETE /api/v1/grade-subjects/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        log.info("REST request to delete GradeSubject ID: {}", id);
        gradeSubjectService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Xóa cấu hình thành công!"));
    }
}
