package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.QuestionDto;
import com.lms.education.module.lms.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasAuthority('LMS_QUESTION_CREATE')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "question", description = "Tạo mới câu hỏi trong ngân hàng câu hỏi")
    public ResponseEntity<Map<String, Object>> createQuestion(
            @Valid @ModelAttribute QuestionDto dto,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        QuestionDto created = questionService.create(dto, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Tạo mới câu hỏi thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "question", description = "Cập nhật câu hỏi")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable Long id,
            @ModelAttribute QuestionDto dto,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        QuestionDto updated = questionService.update(id, dto, file);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật câu hỏi thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_DELETE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "question", description = "Xóa câu hỏi")
    public ResponseEntity<Map<String, String>> deleteQuestion(@PathVariable Long id) {
        questionService.delete(id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa câu hỏi thành công!");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_VIEW')")
    public ResponseEntity<QuestionDto> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_QUESTION_VIEW')")
    public ResponseEntity<List<QuestionDto>> getQuestionsByAssignmentId(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(questionService.getByAssignmentId(assignmentId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LMS_QUESTION_VIEW')")
    public ResponseEntity<Page<QuestionDto>> getAllQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String questionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(questionService.getAll(keyword, questionType, pageable));
    }
}
