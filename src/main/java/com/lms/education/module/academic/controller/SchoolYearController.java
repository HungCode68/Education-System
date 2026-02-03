package com.lms.education.module.academic.controller;
import com.lms.education.module.academic.entity.SchoolYear.SchoolYearStatus;
import com.lms.education.module.academic.dto.SchoolYearDto;
import com.lms.education.module.academic.service.SchoolYearService;
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
@RequestMapping("/api/v1/school-years")
@RequiredArgsConstructor
@Slf4j
public class SchoolYearController {

    private final SchoolYearService schoolYearService;

    //  Lấy danh sách tất cả
    @GetMapping
    public ResponseEntity<PageResponse<SchoolYearDto>> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) SchoolYearStatus status,
            @RequestParam(defaultValue = "1") int page, // Mặc định trang 1
            @RequestParam(defaultValue = "10") int size // Mặc định 10 dòng/trang
    ) {
        log.info("REST request to search SchoolYears");
        return ResponseEntity.ok(schoolYearService.getAll(keyword, status, page, size));
    }

    //  Lấy chi tiết theo ID
    @GetMapping("/{id}")
    public ResponseEntity<SchoolYearDto> getById(@PathVariable String id) {
        log.info("REST request to get SchoolYear ID: {}", id);
        return ResponseEntity.ok(schoolYearService.getById(id));
    }

    //  Lấy năm học hiện tại (Theo ngày hôm nay)
    @GetMapping("/current")
    public ResponseEntity<SchoolYearDto> getCurrent() {
        log.info("REST request to get Current SchoolYear");
        SchoolYearDto current = schoolYearService.getCurrentSchoolYear();
        if (current == null) {
            return ResponseEntity.noContent().build(); // Trả về 204 nếu không tìm thấy
        }
        return ResponseEntity.ok(current);
    }

    //  Tạo mới
    @PostMapping
    public ResponseEntity<SchoolYearDto> create(@Valid @RequestBody SchoolYearDto dto) {
        log.info("REST request to create SchoolYear: {}", dto.getName());
        // Trả về 201 Created thay vì 200 OK
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolYearService.create(dto));
    }

    //  Cập nhật
    @PutMapping("/{id}")
    public ResponseEntity<SchoolYearDto> update(
            @PathVariable String id,
            @Valid @RequestBody SchoolYearDto dto
    ) {
        log.info("REST request to update SchoolYear ID: {}", id);
        return ResponseEntity.ok(schoolYearService.update(id, dto));
    }

    //  Xóa Đơn (Theo ID trên URL)
    // API: DELETE /api/v1/school-years/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteOne(@PathVariable String id) {
        log.info("REST request to delete SchoolYear ID: {}", id);
        // Bọc ID đơn lẻ vào List để gọi hàm chung của Service
        schoolYearService.delete(List.of(id));
        return ResponseEntity.ok(Map.of("message", "Xóa năm học thành công!"));
    }

    //  Xóa Nhiều (Bulk Delete)
    // API: DELETE /api/v1/school-years
    // Body: ["id1", "id2", "id3"]
    @DeleteMapping
    public ResponseEntity<Object> deleteMany(@RequestBody List<String> ids) {
        log.info("REST request to bulk delete SchoolYears: {}", ids);
        schoolYearService.delete(ids);
        return ResponseEntity.ok(Map.of("message", "Xóa năm học thành công!"));
    }

    //  API Kết thúc năm học
    // API: PUT /api/v1/school-years/{id}/archive
    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable String id) {
        log.info("REST request to archive SchoolYear ID: {}", id);

        schoolYearService.archive(id);
        return ResponseEntity.noContent().build();
    }
}
