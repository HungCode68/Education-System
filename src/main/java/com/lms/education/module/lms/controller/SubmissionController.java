package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.SubmissionDto;
import com.lms.education.module.lms.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/start/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_START')")
    @LogActivity(module = "LMS", action = "START", targetType = "submission", description = "Học viên bắt đầu làm bài tập")
    public ResponseEntity<Map<String, Object>> startSubmission(@PathVariable Long assignmentId) {
        SubmissionDto created = submissionService.startSubmission(assignmentId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Bắt đầu làm bài tập thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_SUBMIT')")
    @LogActivity(module = "LMS", action = "SUBMIT", targetType = "submission", description = "Học viên nộp bài tập")
    public ResponseEntity<Map<String, Object>> submitAssignment(@PathVariable Long id) {
        SubmissionDto submitted = submissionService.submitAssignment(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Nộp bài tập thành công!");
        response.put("data", submitted);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/grade/{id}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_GRADE')")
    @LogActivity(module = "LMS", action = "GRADE", targetType = "submission", description = "Giảng viên chấm điểm bài làm học viên")
    public ResponseEntity<Map<String, Object>> gradeSubmission(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal score,
            @RequestParam(required = false) String feedback) {

        SubmissionDto graded = submissionService.gradeSubmission(id, score, feedback);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chấm điểm bài làm thành công!");
        response.put("data", graded);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_VIEW')")
    public ResponseEntity<SubmissionDto> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getById(id));
    }

    @GetMapping("/my-submission/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_VIEW')")
    public ResponseEntity<SubmissionDto> getMySubmission(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(submissionService.getMySubmission(assignmentId));
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_VIEW')")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByAssignmentId(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(submissionService.getByAssignmentId(assignmentId));
    }

    @GetMapping("/assignment/{assignmentId}/page")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_VIEW')")
    public ResponseEntity<Page<SubmissionDto>> getSubmissionsByAssignmentIdPageable(
            @PathVariable Long assignmentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(submissionService.getByAssignmentIdPageable(assignmentId, pageable));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('LMS_SUBMISSION_VIEW')")
    public ResponseEntity<List<SubmissionDto>> getSubmissionsByStudentId(@PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.getByStudentId(studentId));
    }
}
