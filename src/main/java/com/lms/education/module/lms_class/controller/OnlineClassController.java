package com.lms.education.module.lms_class.controller;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.lms_class.dto.OnlineClassDto;
import com.lms.education.module.lms_class.service.OnlineClassService;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.utils.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/online-classes")
@RequiredArgsConstructor
@Slf4j
public class OnlineClassController {

    private final OnlineClassService onlineClassService;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;

    // =================================================================
    // DÀNH CHO GIÁO VIÊN
    // =================================================================

    // Lấy danh sách lớp tôi dạy (My Classes)
    // GET /api/v1/online-classes/teacher/my-classes
    @GetMapping("/teacher/my-classes")
    @PreAuthorize("hasAuthority('ONLINE_CLASS_VIEW')")
    public ResponseEntity<List<OnlineClassDto>> getMyClasses(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        // Tìm Teacher từ token
        Teacher teacher = teacherRepository.findByUser_Email(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giáo viên"));

        // Truyền teacher.getId() vào service
        return ResponseEntity.ok(onlineClassService.getMyClasses(teacher.getId()));
    }

    // =================================================================
    // DÀNH CHO ADMIN / QUẢN LÝ
    // =================================================================

    // Tìm kiếm và phân trang
    // GET /api/v1/online-classes?keyword=Toan&status=active&page=1&size=10
    @GetMapping
    @PreAuthorize("hasAuthority('ONLINE_CLASS_VIEW')")
    public ResponseEntity<PageResponse<OnlineClassDto>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(onlineClassService.search(keyword, status, page, size));
    }

    // Xem chi tiết một lớp
    // GET /api/v1/online-classes/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ONLINE_CLASS_VIEW')")
    public ResponseEntity<OnlineClassDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(onlineClassService.getById(id));
    }

    // Cập nhật thông tin lớp (Tên, Trạng thái)
    // PUT /api/v1/online-classes/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ONLINE_CLASS_VIEW')")
    public ResponseEntity<OnlineClassDto> update(
            @PathVariable String id,
            @Valid @RequestBody OnlineClassDto dto
    ) {
        log.info("Request cập nhật Online Class ID: {}", id);
        return ResponseEntity.ok(onlineClassService.update(id, dto));
    }

    // LƯU Ý: Không có API Create (POST)
    // Vì lớp học được tạo tự động khi Phân công giảng dạy (TeachingAssignment).
}
