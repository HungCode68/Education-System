package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.SemesterDto;
import com.lms.education.module.academic.entity.Semester;
import com.lms.education.module.academic.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/semesters")
@RequiredArgsConstructor
@Slf4j
public class SemesterController {

    private final SemesterService semesterService;

    // Tạo mới Học kỳ
    // POST /api/v1/semesters
    @PostMapping
    public ResponseEntity<SemesterDto> create(@Valid @RequestBody SemesterDto dto) {
        log.info("Request tạo học kỳ mới: {}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(semesterService.create(dto));
    }

    // Cập nhật thông tin Học kỳ
    // PUT /api/v1/semesters/{id}
    @PutMapping("/{id}")
    public ResponseEntity<SemesterDto> update(@PathVariable String id, @Valid @RequestBody SemesterDto dto) {
        log.info("Request cập nhật học kỳ ID: {}", id);
        return ResponseEntity.ok(semesterService.update(id, dto));
    }

    // Xóa Học kỳ
    // DELETE /api/v1/semesters/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) {
        semesterService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa học kỳ thành công"));
    }

    // Xem chi tiết
    // GET /api/v1/semesters/{id}
    @GetMapping("/{id}")
    public ResponseEntity<SemesterDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(semesterService.getById(id));
    }

    // Lấy danh sách Học kỳ theo Năm học (Sắp xếp theo thứ tự ưu tiên)
    // GET /api/v1/semesters/by-year/{schoolYearId}
    @GetMapping("/by-year/{schoolYearId}")
    public ResponseEntity<List<SemesterDto>> getBySchoolYear(@PathVariable String schoolYearId) {
        return ResponseEntity.ok(semesterService.getAllBySchoolYear(schoolYearId));
    }

    //  Cập nhật trạng thái nhanh (Kích hoạt / Kết thúc)
    // PATCH /api/v1/semesters/{id}/status?status=active
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String id,
            @RequestParam String status // active, upcoming, finished
    ) {
        try {
            Semester.SemesterStatus statusEnum = Semester.SemesterStatus.valueOf(status);
            semesterService.updateStatus(id, statusEnum);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không hợp lệ (active, upcoming, finished)"));
        }
    }
}
