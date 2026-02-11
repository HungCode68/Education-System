package com.lms.education.module.teaching_assignment.controller;

import com.lms.education.module.teaching_assignment.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import com.lms.education.module.teaching_assignment.service.TeachingSubstitutionService;
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
@RequestMapping("/api/v1/teaching-substitutions")
@RequiredArgsConstructor
@Slf4j
public class TeachingSubstitutionController {

    private final TeachingSubstitutionService substitutionService;

    // Tạo yêu cầu dạy thay mới
    // POST /api/v1/teaching-substitutions
    @PostMapping
    public ResponseEntity<TeachingSubstitutionDto> create(@Valid @RequestBody TeachingSubstitutionDto dto) {
        log.info("Request tạo dạy thay cho phân công ID: {}", dto.getAssignmentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(substitutionService.create(dto));
    }

    // Tìm kiếm và Phân trang (Cho Admin/Quản lý)
    // GET /api/v1/teaching-substitutions?schoolYearId=...&semesterId=...&keyword=...
    @GetMapping
    public ResponseEntity<PageResponse<TeachingSubstitutionDto>> search(
            @RequestParam(required = false) String schoolYearId,
            @RequestParam(required = false) String semesterId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(substitutionService.search(schoolYearId, semesterId, keyword, page, size));
    }

    // Xem lịch dạy thay của một giáo viên cụ thể
    // GET /api/v1/teaching-substitutions/by-teacher/{teacherId}
    @GetMapping("/by-teacher/{teacherId}")
    public ResponseEntity<List<TeachingSubstitutionDto>> getBySubTeacher(@PathVariable String teacherId) {
        return ResponseEntity.ok(substitutionService.getBySubTeacher(teacherId));
    }

    // Cập nhật trạng thái (Duyệt / Từ chối / Hủy)
    // PATCH /api/v1/teaching-substitutions/{id}/status?status=rejected
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String id,
            @RequestParam String status // pending, approved, cancelled, rejected
    ) {
        try {
            TeachingSubstitution.SubstitutionStatus statusEnum = TeachingSubstitution.SubstitutionStatus.valueOf(status);
            substitutionService.updateStatus(id, statusEnum);
            return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công: " + status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Trạng thái không hợp lệ."));
        }
    }

    // Hủy yêu cầu dạy thay (Shortcut cho việc set status = cancelled)
    // DELETE /api/v1/teaching-substitutions/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable String id) {
        substitutionService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Đã hủy yêu cầu dạy thay thành công"));
    }
}