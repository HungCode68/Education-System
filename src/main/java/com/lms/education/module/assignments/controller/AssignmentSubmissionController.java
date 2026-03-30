package com.lms.education.module.assignments.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.assignments.dto.AssignmentSubmissionDto;
import com.lms.education.module.assignments.service.AssignmentSubmissionService;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/assignment-submissions")
@RequiredArgsConstructor
@Slf4j
public class AssignmentSubmissionController {

    private final AssignmentSubmissionService submissionService;
    private final StudentRepository studentRepository;

    // DÀNH CHO HỌC SINH

    /**
     * HỌC SINH: LƯU NHÁP HOẶC NỘP BÀI
     */
    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmissionDto> submitAssignment(
            @Valid @RequestBody SubmitRequest request,
            Principal principal) {

        String userId = getUserId(principal);

        // Tự động tìm hồ sơ học sinh dựa trên tài khoản đang đăng nhập
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản này chưa được liên kết với hồ sơ học sinh nào!"));

        log.info("REST request - Học sinh {} đang {} bài tập {}",
                student.getStudentCode(), request.isSubmit() ? "NỘP" : "LƯU NHÁP", request.getAssignmentId());

        AssignmentSubmissionDto result = submissionService.submitAssignment(
                request.getAssignmentId(), student.getId(), request.getStudentNote(), request.isSubmit());

        return ResponseEntity.ok(result);
    }

    /**
     * HỌC SINH: XEM LẠI BÀI ĐÃ NỘP CỦA MÌNH
     */
    @GetMapping("/assignment/{assignmentId}/my-submission")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmissionDto> getMySubmission(
            @PathVariable String assignmentId,
            Principal principal) {

        String userId = getUserId(principal);
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản này chưa được liên kết với hồ sơ học sinh nào!"));

        AssignmentSubmissionDto submission = submissionService.getMySubmission(assignmentId, student.getId());
        return ResponseEntity.ok(submission);
    }


    // DÀNH CHO GIÁO VIÊN / ADMIN

    /**
     * GIÁO VIÊN: LẤY DANH SÁCH BÀI NỘP CỦA 1 BÀI TẬP
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW')")
    public ResponseEntity<Page<AssignmentSubmissionDto>> getSubmissionsByAssignment(
            @PathVariable String assignmentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) com.lms.education.module.assignments.entity.AssignmentSubmission.SubmissionStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        Page<AssignmentSubmissionDto> submissions = submissionService.searchSubmissionsByAssignment(
                assignmentId, keyword, status, pageable);

        return ResponseEntity.ok(submissions);
    }

    /**
     * GIÁO VIÊN: CHẤM ĐIỂM BÀI NỘP (Thủ công)
     */
    @PatchMapping("/{submissionId}/grade")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public ResponseEntity<AssignmentSubmissionDto> gradeSubmission(
            @PathVariable String submissionId,
            @Valid @RequestBody GradeRequest request,
            Principal principal) {

        String graderUserId = getUserId(principal);
        log.info("REST request - Giáo viên {} đang chấm điểm bài nộp ID: {}", graderUserId, submissionId);

        AssignmentSubmissionDto gradedSubmission = submissionService.gradeSubmission(
                submissionId, graderUserId, request.getScore(), request.getFeedback());

        return ResponseEntity.ok(gradedSubmission);
    }

    // CÁC CLASS DTO PHỤ TRỢ (Dùng để hứng dữ liệu từ Request Body)

    @Data
    public static class SubmitRequest {
        @NotNull(message = "ID Bài tập không được để trống")
        private String assignmentId;
        private String studentNote;
        @JsonProperty("isSubmit")
        private boolean isSubmit; // true = Nộp bài chính thức, false = Lưu nháp
    }

    @Data
    public static class GradeRequest {
        @NotNull(message = "Điểm số không được để trống")
        private Double score;
        private String feedback; // Lời phê của giáo viên
    }

    // Hàm Helper lấy ID từ Token
    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}
