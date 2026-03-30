package com.lms.education.module.assignments.controller;

import com.lms.education.module.assignments.dto.SubmissionAnswerDto;
import com.lms.education.module.assignments.service.SubmissionAnswerService;
import com.lms.education.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submission-answers")
@RequiredArgsConstructor
@Slf4j
public class SubmissionAnswerController {

    private final SubmissionAnswerService answerService;

    /**
     * HỌC SINH: LƯU NHÁP ĐÁP ÁN CHO 1 CÂU HỎI
     * Frontend sẽ gọi API này liên tục dưới nền mỗi khi học sinh click chọn đáp án (A,B,C,D) hoặc gõ chữ.
     */
    @PutMapping("/submission/{submissionId}/question/{questionId}")
    @PreAuthorize("hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<SubmissionAnswerDto> saveAnswer(
            @PathVariable String submissionId,
            @PathVariable String questionId,
            @RequestBody SubmissionAnswerDto dto,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang lưu nháp đáp án câu {} của bài nộp {}", userId, questionId, submissionId);

        SubmissionAnswerDto savedAnswer = answerService.saveAnswer(submissionId, questionId, dto, userId);
        return ResponseEntity.ok(savedAnswer);
    }

    /**
     * CHUNG: LẤY TOÀN BỘ BÀI LÀM (CHI TIẾT TỪNG CÂU)
     * - Học sinh xem lại bài
     * - Giáo viên xem bài của học sinh để chấm phần tự luận.
     */
    @GetMapping("/submission/{submissionId}")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW') or hasAuthority('STUDENT_VIEW') or hasRole('STUDENT')")
    public ResponseEntity<List<SubmissionAnswerDto>> getAnswersBySubmission(
            @PathVariable String submissionId,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang xem chi tiết bài làm {}", userId, submissionId);

        List<SubmissionAnswerDto> answers = answerService.getAnswersBySubmission(submissionId, userId);
        return ResponseEntity.ok(answers);
    }

    /**
     * GIÁO VIÊN/ADMIN: KÍCH HOẠT CHẤM ĐIỂM TỰ ĐỘNG (THỦ CÔNG)
     */
    @PostMapping("/submission/{submissionId}/auto-grade")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public ResponseEntity<Map<String, Object>> triggerAutoGrade(
            @PathVariable String submissionId) {

        log.info("REST request - Kích hoạt chấm điểm tự động cho bài nộp {}", submissionId);
        Double totalScore = answerService.autoGradeSubmission(submissionId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã chấm điểm tự động thành công!",
                "totalScore", totalScore
        ));
    }

    // Hàm Helper lấy ID từ Token
    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}