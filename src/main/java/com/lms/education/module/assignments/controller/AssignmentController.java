package com.lms.education.module.assignments.controller;

import com.lms.education.module.assignments.dto.AssignmentDto;
import com.lms.education.module.assignments.service.AssignmentService;
import com.lms.education.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.security.Principal;
import java.util.Map;

import org.springframework.security.core.Authentication;
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
@Slf4j
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentDto> createAssignment(
            @RequestPart("data") @Valid AssignmentDto dto, // Data gửi dưới dạng text/json
            @RequestPart(value = "file", required = false) MultipartFile file, // File đính kèm (có thể null)
            Principal principal) {

        // Lấy ID của người dùng từ Token đang đăng nhập
        String userId = getUserId(principal);

        log.info("REST request - User {} đang tạo bài tập mới", userId);
        AssignmentDto createdAssignment = assignmentService.create(dto, file, userId);
        return new ResponseEntity<>(createdAssignment, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssignmentDto> updateAssignment(
            @PathVariable String id,
            @RequestPart("data") @Valid AssignmentDto dto,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal) {

        String userId = getUserId(principal);
        AssignmentDto updatedAssignment = assignmentService.update(id, dto, file, userId);
        return ResponseEntity.ok(updatedAssignment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAssignment(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        assignmentService.delete(id, userId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa assignment thành công"));
    }

    /**
     * LẤY THÔNG TIN CHI TIẾT 1 BÀI TẬP
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentDto> getAssignmentById(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        AssignmentDto assignment = assignmentService.getById(id, userId);
        return ResponseEntity.ok(assignment);
    }

    /**
     * LẤY DANH SÁCH BÀI TẬP CỦA MỘT LỚP HỌC
     */
    @GetMapping("/class/{classId}")
    public ResponseEntity<Page<AssignmentDto>> getAssignmentsByClass(
            @PathVariable String classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Principal principal) {

        String userId = getUserId(principal);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);


        Page<AssignmentDto> assignments = assignmentService.getAssignmentsByClass(classId, pageable, userId);
        return ResponseEntity.ok(assignments);
    }

    /**
     * LẤY DANH SÁCH BÀI TẬP DO MÌNH TẠO (Dành riêng cho Giáo viên xem lại)
     */
    @GetMapping("/my-assignments")
    public ResponseEntity<Page<AssignmentDto>> getMyAssignments(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        String userId = getUserId(principal);

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AssignmentDto> assignments = assignmentService.getAssignmentsByCreator(userId, pageable);
        return ResponseEntity.ok(assignments);
    }

    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}