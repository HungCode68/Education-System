package com.lms.education.module.academic.controller;

import com.lms.education.module.academic.dto.ClassStudentDto;
import com.lms.education.module.academic.dto.AutoDistributeRequest;
import com.lms.education.module.academic.service.ClassStudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/class-students")
@RequiredArgsConstructor
@Slf4j
public class ClassStudentController {

    private final ClassStudentService classStudentService;

    // ==========================================
    // CÁC API CƠ BẢN (BASIC CRUD)
    // ==========================================

    // Lấy danh sách học sinh của một lớp
    // GET /api/v1/class-students/by-class/{classId}
    @GetMapping("/by-class/{classId}")
    public ResponseEntity<List<ClassStudentDto>> getStudentsByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(classStudentService.getStudentsByClass(classId, status));
    }

    // Thêm thủ công 1 học sinh vào lớp (Dùng khi có học sinh chuyển đến giữa năm)
    // POST /api/v1/class-students
    @PostMapping
    public ResponseEntity<ClassStudentDto> addStudent(@Valid @RequestBody ClassStudentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(classStudentService.addStudentToClass(dto));
    }

    // Xóa học sinh khỏi lớp (Gỡ bỏ)
    // DELETE /api/v1/class-students/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> removeStudent(@PathVariable String id) {
        classStudentService.removeStudentFromClass(id);
        return ResponseEntity.ok(Map.of("message", "Đã xóa học sinh khỏi lớp thành công"));
    }

    // Cập nhật trạng thái (Thôi học, Chuyển trường...)
    // PATCH /api/v1/class-students/{id}/status?status=dropped
    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String id,
            @RequestParam String status // values: studying, transferred, dropped, completed
    ) {
        classStudentService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Cập nhật trạng thái thành công"));
    }

    // ==========================================
    // CÁC CÔNG CỤ TỰ ĐỘNG (AUTOMATION TOOLS)
    // ==========================================

    // Tool 1: Phân lớp tự động (Chia đều học sinh mới vào các lớp)
    // POST /api/v1/class-students/auto-distribute
    @PostMapping("/auto-distribute")
    public ResponseEntity<Map<String, String>> autoDistribute(@RequestBody AutoDistributeRequest request) {
        log.info("Request phân lớp tự động cho {} học sinh.", request.getStudentIds().size());
        Map<String, String> report = classStudentService.autoDistributeStudents(request);
        return ResponseEntity.ok(report);
    }

    // Tool 2: Lên lớp tự động (Bê toàn bộ lớp cũ sang lớp mới)
    // POST /api/v1/class-students/promote?oldClassId=...&newClassId=...
    @PostMapping("/promote")
    public ResponseEntity<Map<String, String>> promoteStudents(
            @RequestParam String oldClassId,
            @RequestParam String newClassId
    ) {
        log.info("Request lên lớp từ {} sang {}", oldClassId, newClassId);
        classStudentService.promoteStudents(oldClassId, newClassId);
        return ResponseEntity.ok(Map.of("message", "Đã thực hiện lên lớp thành công!"));
    }
}
