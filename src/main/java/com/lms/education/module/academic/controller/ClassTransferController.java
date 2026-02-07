package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.ClassTransferHistoryDto;
import com.lms.education.module.academic.dto.TransferStudentRequest;
// Lưu ý: Nếu bạn để file Request này trong package 'dto.request' thì sửa lại import nhé!
import com.lms.education.module.academic.service.ClassTransferService;
import com.lms.education.utils.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/class-transfers")
@RequiredArgsConstructor
@Slf4j
public class ClassTransferController {

    private final ClassTransferService classTransferService;

    // Thực hiện chuyển lớp (API Quan trọng)
    // POST /api/v1/class-transfers
    @PostMapping
    public ResponseEntity<Map<String, String>> transferStudent(@Valid @RequestBody TransferStudentRequest request) {
        log.info("REST request to transfer student: {} to class: {}", request.getStudentId(), request.getToClassId());
        classTransferService.transferStudent(request);
        return ResponseEntity.ok(Map.of("message", "Chuyển lớp thành công!"));
    }

    // Xem lịch sử chuyển lớp của riêng 1 học sinh
    // GET /api/v1/class-transfers/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ClassTransferHistoryDto>> getHistoryByStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(classTransferService.getHistoryByStudent(studentId));
    }

    // Tìm kiếm & Thống kê lịch sử (Dùng cho trang Quản lý chuyển lớp)
    // GET /api/v1/class-transfers?keyword=...&classId=...&startDate=2025-01-01&endDate=2025-12-31
    @GetMapping
    public ResponseEntity<PageResponse<ClassTransferHistoryDto>> searchHistory(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String classId,

            // Format ngày tháng trên URL: yyyy-MM-dd (VD: 2025-10-20)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(classTransferService.searchHistory(keyword, classId, startDate, endDate, page, size));
    }
}
