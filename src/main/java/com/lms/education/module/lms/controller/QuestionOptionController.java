package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.QuestionOptionDto;
import com.lms.education.module.lms.service.QuestionOptionService;
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
@RequestMapping("/api/v1/question-options")
@RequiredArgsConstructor
public class QuestionOptionController {

    private final QuestionOptionService questionOptionService;

    @GetMapping("/question/{questionId}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_VIEW')")
    public ResponseEntity<List<QuestionOptionDto>> getOptionsByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(questionOptionService.getByQuestionId(questionId));
    }

    @PostMapping("/question/{questionId}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_CREATE')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "question_option", description = "Tạo mới lựa chọn cho câu hỏi")
    public ResponseEntity<Map<String, Object>> createOption(
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionOptionDto dto) {

        QuestionOptionDto created = questionOptionService.create(questionId, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo mới lựa chọn thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "question_option", description = "Cập nhật lựa chọn cho câu hỏi")
    public ResponseEntity<Map<String, Object>> updateOption(
            @PathVariable Long id,
            @RequestBody QuestionOptionDto dto) {

        QuestionOptionDto updated = questionOptionService.update(id, dto);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật lựa chọn thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_DELETE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "question_option", description = "Xóa lựa chọn cho câu hỏi")
    public ResponseEntity<Map<String, String>> deleteOption(@PathVariable Long id) {
        questionOptionService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa lựa chọn thành công!");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/question/{questionId}/batch")
    @PreAuthorize("hasAuthority('LMS_QUESTION_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "question_option", description = "Cập nhật hàng loạt lựa chọn cho câu hỏi")
    public ResponseEntity<Map<String, Object>> batchReplaceOptions(
            @PathVariable Long questionId,
            @RequestBody List<QuestionOptionDto> dtos) {

        List<QuestionOptionDto> updatedList = questionOptionService.replaceAllForQuestion(questionId, dtos);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật danh sách lựa chọn thành công!");
        response.put("data", updatedList);

        return ResponseEntity.ok(response);
    }
}
