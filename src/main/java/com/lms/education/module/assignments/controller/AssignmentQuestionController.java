package com.lms.education.module.assignments.controller;

import com.lms.education.module.assignments.dto.AssignmentQuestionDto;
import com.lms.education.module.assignments.service.AssignmentQuestionService;
import com.lms.education.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/assignment-questions")
@RequiredArgsConstructor
@Slf4j
public class AssignmentQuestionController {

    private final AssignmentQuestionService questionService;

    /**
     * TẠO MỚI MỘT CÂU HỎI CHO BÀI TẬP
     */
    @PostMapping
    public ResponseEntity<AssignmentQuestionDto> createQuestion(
            @Valid @RequestBody AssignmentQuestionDto dto,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang tạo câu hỏi mới cho bài tập {}", userId, dto.getAssignmentId());

        AssignmentQuestionDto createdQuestion = questionService.create(dto, userId);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    /**
     * CẬP NHẬT CÂU HỎI
     */
    @PutMapping("/{id}")
    public ResponseEntity<AssignmentQuestionDto> updateQuestion(
            @PathVariable String id,
            @Valid @RequestBody AssignmentQuestionDto dto,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang cập nhật câu hỏi ID: {}", userId, id);

        AssignmentQuestionDto updatedQuestion = questionService.update(id, dto, userId);
        return ResponseEntity.ok(updatedQuestion);
    }

    /**
     * XÓA CÂU HỎI
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @PathVariable String id,
            Principal principal) {

        String userId = getUserId(principal);
        log.info("REST request - User {} đang xóa câu hỏi ID: {}", userId, id);

        questionService.delete(id, userId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa question thành công"));
    }

    /**
     * LẤY CHI TIẾT 1 CÂU HỎI
     */
    @GetMapping("/{id}")
    public ResponseEntity<AssignmentQuestionDto> getQuestionById(@PathVariable String id) {
        AssignmentQuestionDto question = questionService.getById(id);
        return ResponseEntity.ok(question);
    }

    /**
     * LẤY TOÀN BỘ CÂU HỎI CỦA 1 BÀI TẬP (Sắp xếp theo thứ tự)
     * API này sẽ được gọi khi học sinh bấm "Bắt đầu làm bài"
     */
    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<AssignmentQuestionDto>> getQuestionsByAssignment(
            @PathVariable String assignmentId) {

        List<AssignmentQuestionDto> questions = questionService.getByAssignmentId(assignmentId);
        return ResponseEntity.ok(questions);
    }

    // Hàm Helper lấy ID từ Token
    private String getUserId(Principal principal) {
        UserPrincipal userPrincipal = (UserPrincipal) ((Authentication) principal).getPrincipal();
        return userPrincipal.getId();
    }
}
