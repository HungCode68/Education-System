package com.lms.education.module.lms_class.controller;

import com.lms.education.module.lms_class.dto.OnlineClassStudentDto;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;
import com.lms.education.module.lms_class.service.OnlineClassStudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/online-classes")
@RequiredArgsConstructor
@Slf4j
public class OnlineClassStudentController {

    private final OnlineClassStudentService onlineClassStudentService;

    // QUẢN LÝ THÀNH VIÊN TRONG LỚP (DÀNH CHO GIÁO VIÊN)

    // Lấy danh sách học sinh trong lớp
    // GET /api/v1/online-classes/{classId}/students?status=active
    @GetMapping("/{classId}/students")
    public ResponseEntity<List<OnlineClassStudentDto>> getStudentsByClass(
            @PathVariable String classId,
            @RequestParam(required = false) String status // active, removed
    ) {
        OnlineClassStudent.StudentStatus statusEnum = null;
        if (status != null) {
            try {
                statusEnum = OnlineClassStudent.StudentStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                // Nếu status sai thì mặc định lấy tất cả hoặc trả lỗi tuỳ bạn
                // Ở đây mình để null để lấy tất cả
            }
        }
        return ResponseEntity.ok(onlineClassStudentService.getStudentsByClass(classId, statusEnum));
    }

    // Thêm học sinh thủ công (Manual Add)
    // POST /api/v1/online-classes/{classId}/students
    // Body: { "studentId": "..." }
    @PostMapping("/{classId}/students")
    public ResponseEntity<OnlineClassStudentDto> addStudentManual(
            @PathVariable String classId,
            @RequestBody OnlineClassStudentDto dto
    ) {
        log.info("Thêm thủ công học sinh {} vào lớp {}", dto.getStudentId(), classId);
        // Lấy studentId từ body, classId từ path
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(onlineClassStudentService.addStudentManual(classId, dto.getStudentId()));
    }

    // Xóa học sinh khỏi lớp (Soft Delete)
    // DELETE /api/v1/online-classes/{classId}/students/{studentId}
    @DeleteMapping("/{classId}/students/{studentId}")
    public ResponseEntity<Map<String, String>> removeStudent(
            @PathVariable String classId,
            @PathVariable String studentId
    ) {
        onlineClassStudentService.removeStudent(classId, studentId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa học sinh khỏi lớp thành công"));
    }

    // Kích hoạt đồng bộ lại danh sách từ Lớp vật lý (Manual Sync Trigger)
    // POST /api/v1/online-classes/{classId}/sync-students
    // Dùng khi lớp vật lý có thay đổi mà lớp Online chưa cập nhật kịp
    @PostMapping("/{classId}/sync-students")
    public ResponseEntity<Map<String, String>> syncStudents(@PathVariable String classId) {
        onlineClassStudentService.syncStudentsFromPhysicalClass(classId);
        return ResponseEntity.ok(Map.of("message", "Đã đồng bộ danh sách học sinh thành công"));
    }

    // =================================================================
    // DÀNH CHO HỌC SINH (MY DASHBOARD)
    // =================================================================

    // Lấy danh sách lớp tôi đang học
    // GET /api/v1/online-classes/student/{studentId}
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<OnlineClassStudentDto>> getMyClasses(@PathVariable String studentId) {
        return ResponseEntity.ok(onlineClassStudentService.getClassesByStudent(studentId));
    }
}
