package com.lms.education.module.assignments.controller;

import com.lms.education.module.assignments.dto.QuestionOptionDto;
import com.lms.education.module.assignments.service.QuestionOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Slf4j
public class QuestionOptionController {

    private final QuestionOptionService questionOptionService;

    /**
     * LẤY DANH SÁCH ĐÁP ÁN CỦA MỘT CÂU HỎI
     * Sắp xếp theo thứ tự A, B, C, D (displayOrder)
     */
    @GetMapping("/{questionId}/options")
    @PreAuthorize("hasAuthority('ASSIGNMENT_VIEW') or hasAuthority('STUDENT_VIEW')")
    // Học sinh cũng cần quyền xem đáp án để làm bài
    public ResponseEntity<List<QuestionOptionDto>> getOptionsByQuestion(@PathVariable String questionId) {
        List<QuestionOptionDto> options = questionOptionService.getByQuestionId(questionId);
        return ResponseEntity.ok(options);
    }

    /**
     * LƯU HÀNG LOẠT ĐÁP ÁN CHO MỘT CÂU HỎI (Dùng cho cả Tạo mới và Cập nhật)
     * Hệ thống sẽ tự động xóa đáp án cũ và thay bằng danh sách mới này.
     */
    @PutMapping("/{questionId}/options")
    @PreAuthorize("hasAuthority('ASSIGNMENT_UPDATE')")
    public ResponseEntity<List<QuestionOptionDto>> saveOptionsForQuestion(
            @PathVariable String questionId,
            @Valid @RequestBody List<QuestionOptionDto> optionDtos) {

        log.info("REST request - Lưu hàng loạt đáp án cho Câu hỏi ID: {}", questionId);
        List<QuestionOptionDto> savedOptions = questionOptionService.saveOptionsForQuestion(questionId, optionDtos);

        return new ResponseEntity<>(savedOptions, HttpStatus.OK);
    }

    /**
     * XÓA TOÀN BỘ ĐÁP ÁN CỦA MỘT CÂU HỎI
     */
    @DeleteMapping("/{questionId}/options")
    @PreAuthorize("hasAuthority('ASSIGNMENT_DELETE')")
    public ResponseEntity<Map<String, String>> deleteAllOptionsByQuestion(@PathVariable String questionId) {
        log.info("REST request - Xóa toàn bộ đáp án của Câu hỏi ID: {}", questionId);
        questionOptionService.deleteByQuestionId(questionId);

        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ đáp án của câu hỏi này thành công"));
    }
}
