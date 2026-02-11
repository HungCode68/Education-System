package com.lms.education.module.teaching_assignment.controller;

import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching_assignment.dto.TeachingAssignmentHistoryDto;
import com.lms.education.module.teaching_assignment.service.TeachingAssignmentHistoryService;
import com.lms.education.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teaching-assignment-history")
@RequiredArgsConstructor
@Slf4j
public class TeachingAssignmentHistoryController {

    private final TeachingAssignmentHistoryService historyService;

    // Tìm kiếm và Phân trang (Cho màn hình "Nhật ký hệ thống" của Admin)
    // GET /api/v1/teaching-assignment-history?keyword=Toan&page=1&size=20
    @GetMapping
    public ResponseEntity<PageResponse<TeachingAssignmentHistoryDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(historyService.search(keyword, page, size));
    }

    // Xem lịch sử của một Phân công cụ thể
    // GET /api/v1/teaching-assignment-history/assignment/{assignmentId}
    // Dùng khi: Admin bấm vào nút "Lịch sử" trên dòng phân công môn Toán 10A1
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<TeachingAssignmentHistoryDto>> getByAssignment(@PathVariable String assignmentId) {
        return ResponseEntity.ok(historyService.getByAssignment(assignmentId));
    }

    // Xem lịch sử biến động của một Lớp học
    // GET /api/v1/teaching-assignment-history/class/{classId}
    // Dùng khi: Vào chi tiết lớp 10A1 -> Tab "Lịch sử thay đổi GV"
    @GetMapping("/class/{classId}")
    public ResponseEntity<List<TeachingAssignmentHistoryDto>> getByClass(@PathVariable String classId) {
        return ResponseEntity.ok(historyService.getByClass(classId));
    }

    // Xem lịch sử biến động của một Giáo viên
    // GET /api/v1/teaching-assignment-history/teacher/{teacherId}
    // Dùng khi: Vào hồ sơ Cô Lan -> Xem "Lịch sử công tác"
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeachingAssignmentHistoryDto>> getByTeacher(@PathVariable String teacherId) {
        return ResponseEntity.ok(historyService.getByTeacher(teacherId));
    }
}
