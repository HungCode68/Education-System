package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.PhysicalClassDto;
import com.lms.education.module.academic.service.PhysicalClassService;
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
@RequestMapping("/api/v1/physical-classes")
@RequiredArgsConstructor
@Slf4j
public class PhysicalClassController {

    private final PhysicalClassService physicalClassService;

    // Tìm kiếm và Phân trang
    // GET /api/v1/physical-classes?schoolYearId=...&gradeId=...&keyword=10A1&page=1
    @GetMapping
    public ResponseEntity<PageResponse<PhysicalClassDto>> search(
            @RequestParam(required = false) String schoolYearId,
            @RequestParam(required = false) String gradeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("REST request to search Classes. Year: {}, Grade: {}, Key: {}", schoolYearId, gradeId, keyword);
        return ResponseEntity.ok(physicalClassService.search(schoolYearId, gradeId, keyword, page, size));
    }

    // Lấy danh sách lớp cho Dropdown (Khi chọn Năm học -> Chọn Khối -> Load list lớp)
    // GET /api/v1/physical-classes/dropdown?schoolYearId=...&gradeId=...
    @GetMapping("/dropdown")
    public ResponseEntity<List<PhysicalClassDto>> getDropdown(
            @RequestParam String schoolYearId,
            @RequestParam String gradeId
    ) {
        return ResponseEntity.ok(physicalClassService.getDropdownList(schoolYearId, gradeId));
    }

    // Lấy chi tiết lớp
    // GET /api/v1/physical-classes/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PhysicalClassDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(physicalClassService.getById(id));
    }

    // Tạo lớp mới
    // POST /api/v1/physical-classes
    @PostMapping
    public ResponseEntity<PhysicalClassDto> create(@Valid @RequestBody PhysicalClassDto dto) {
        log.info("REST request to create Class: {}", dto.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(physicalClassService.create(dto));
    }

    // Cập nhật thông tin lớp (Đổi tên, đổi phòng, đổi GVCN)
    // PUT /api/v1/physical-classes/{id}
    @PutMapping("/{id}")
    public ResponseEntity<PhysicalClassDto> update(
            @PathVariable String id,
            @Valid @RequestBody PhysicalClassDto dto
    ) {
        log.info("REST request to update Class ID: {}", id);
        return ResponseEntity.ok(physicalClassService.update(id, dto));
    }

    // Xóa lớp (Chỉ xóa khi chưa có dữ liệu ràng buộc)
    // DELETE /api/v1/physical-classes/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable String id) {
        log.info("REST request to delete Class ID: {}", id);
        physicalClassService.delete(id);

        // Trả về JSON message thành công (như bạn yêu cầu)
        return ResponseEntity.ok(Map.of("message", "Xóa lớp học thành công!"));
    }
}
