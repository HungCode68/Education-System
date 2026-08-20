package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.SubmissionAnswerDto;
import com.lms.education.module.lms.service.SubmissionAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submission-answers")
@RequiredArgsConstructor
public class SubmissionAnswerController {

    private final SubmissionAnswerService submissionAnswerService;

    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_VIEW', 'LMS_SUBMISSION_CREATE', 'LMS_SUBMISSION_UPDATE', 'LMS_SUBMISSION_SUBMIT')")
    public ResponseEntity<List<SubmissionAnswerDto>> getAnswersBySubmissionId(@PathVariable Long submissionId) {
        return ResponseEntity.ok(submissionAnswerService.getAnswersBySubmissionId(submissionId));
    }

    @PostMapping("/submission/{submissionId}")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_CREATE', 'LMS_SUBMISSION_UPDATE', 'LMS_SUBMISSION_SUBMIT')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "submission_answer", description = "Lưu hoặc cập nhật câu trả lời trong bài nộp")
    public ResponseEntity<Map<String, Object>> saveOrUpdateAnswer(
            @PathVariable Long submissionId,
            @Valid @RequestBody SubmissionAnswerDto dto) {

        SubmissionAnswerDto saved = submissionAnswerService.saveOrUpdateAnswer(submissionId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lưu câu trả lời thành công!");
        response.put("data", saved);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/submission/{submissionId}/batch")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_CREATE', 'LMS_SUBMISSION_UPDATE', 'LMS_SUBMISSION_SUBMIT')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "submission_answer", description = "Lưu hàng loạt câu trả lời và tự động chấm điểm")
    public ResponseEntity<Map<String, Object>> batchSaveAnswers(
            @PathVariable Long submissionId,
            @RequestBody List<SubmissionAnswerDto> dtos) {

        List<SubmissionAnswerDto> savedList = submissionAnswerService.batchSaveAnswers(submissionId, dtos);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Lưu bộ câu trả lời và tự động tính điểm thành công!");
        response.put("data", savedList);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/submission/{submissionId}/grade-batch")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_GRADE')")
    @LogActivity(module = "LMS", action = "GRADE", targetType = "submission_answer", description = "Giáo viên chấm điểm hàng loạt các câu trả lời")
    public ResponseEntity<Map<String, Object>> batchGradeAnswers(
            @PathVariable Long submissionId,
            @RequestBody List<com.lms.education.module.lms.dto.GradeAnswerDto> grades) {

        List<SubmissionAnswerDto> gradedList = submissionAnswerService.batchGradeAnswers(submissionId, grades);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chấm điểm hàng loạt thành công!");
        response.put("data", gradedList);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/submission/{submissionId}/question/{questionId}")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_UPDATE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "submission_answer", description = "Xóa câu trả lời khỏi bài nộp")
    public ResponseEntity<Map<String, Object>> removeAnswer(
            @PathVariable Long submissionId,
            @PathVariable Long questionId) {

        submissionAnswerService.removeAnswer(submissionId, questionId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Xóa câu trả lời thành công!");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{answerId}/grade")
    @PreAuthorize("hasAnyAuthority('LMS_SUBMISSION_UPDATE', 'LMS_SUBMISSION_CREATE')")
    @LogActivity(module = "LMS", action = "GRADE", targetType = "submission_answer", description = "Giáo viên chấm điểm cho câu trả lời tự luận")
    public ResponseEntity<Map<String, Object>> gradeAnswer(
            @PathVariable Long answerId,
            @RequestParam java.math.BigDecimal score) {

        SubmissionAnswerDto graded = submissionAnswerService.gradeAnswer(answerId, score);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chấm điểm câu trả lời thành công!");
        response.put("data", graded);

        return ResponseEntity.ok(response);
    }
}

