package com.lms.education.module.lms.controller;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.lms.dto.AssignmentQuestionDto;
import com.lms.education.module.lms.service.AssignmentQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lms.education.module.lms.repository.SubmissionRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.entity.Student;


@RestController
@RequestMapping("/api/v1/assignment-questions")
@RequiredArgsConstructor
public class AssignmentQuestionController {

    
    private final AssignmentQuestionService assignmentQuestionService;
    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;


@GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_VIEW')")
    public ResponseEntity<List<AssignmentQuestionDto>> getQuestionsByAssignmentId(@PathVariable Long assignmentId) {
        List<AssignmentQuestionDto> list = assignmentQuestionService.getByAssignmentId(assignmentId);
        
        // Check if current user is a teacher/admin (who can manage/view full details)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isTeacherOrAdmin = false;
        if (auth != null && auth.getAuthorities() != null) {
            isTeacherOrAdmin = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_TEACHER") || a.equals("ROLE_ADMIN") || a.equals("ROLE_STAFF") || a.equals("LMS_ASSIGNMENT_MANAGE"));
        }

        // Hide isCorrect for students, but keep it for teachers
        for (AssignmentQuestionDto aq : list) {
            if (aq.getQuestion() != null && aq.getQuestion().getOptions() != null) {
                int correctCount = 0;
                for (com.lms.education.module.lms.dto.QuestionOptionDto opt : aq.getQuestion().getOptions()) {
                    if (Boolean.TRUE.equals(opt.getIsCorrect())) {
                        correctCount++;
                    }
                    if (!isTeacherOrAdmin) {
                        opt.setIsCorrect(null); // Luôn ẩn isCorrect đối với Học viên
                    }
                }
                aq.setAllowMultipleAnswers(correctCount > 1);
            } else {
                aq.setAllowMultipleAnswers(false);
            }
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_UPDATE')")
    @LogActivity(module = "LMS", action = "CREATE", targetType = "assignment_question", description = "Thêm câu hỏi vào bài tập")
    public ResponseEntity<Map<String, Object>> addQuestionToAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentQuestionDto dto) {

        dto.setAssignmentId(assignmentId);
        AssignmentQuestionDto created = assignmentQuestionService.addQuestionToAssignment(assignmentId, dto);


        Map<String, Object> response = new HashMap<>();
        response.put("message", "Thêm câu hỏi vào bài tập thành công!");
        response.put("data", created);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/assignment/{assignmentId}/question/{questionId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "assignment_question", description = "Cập nhật thứ tự hoặc điểm số câu hỏi trong bài tập")
    public ResponseEntity<Map<String, Object>> updateQuestionInAssignment(
            @PathVariable Long assignmentId,
            @PathVariable Long questionId,
            @RequestBody AssignmentQuestionDto dto) {

        AssignmentQuestionDto updated = assignmentQuestionService.updateQuestionInAssignment(
                assignmentId, questionId, dto.getOrderNumber(), dto.getScoreWeight());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật câu hỏi trong bài tập thành công!");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/assignment/{assignmentId}/question/{questionId}")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_UPDATE')")
    @LogActivity(module = "LMS", action = "DELETE", targetType = "assignment_question", description = "Xóa câu hỏi khỏi bài tập")
    public ResponseEntity<Map<String, String>> removeQuestionFromAssignment(
            @PathVariable Long assignmentId,
            @PathVariable Long questionId) {

        assignmentQuestionService.removeQuestionFromAssignment(assignmentId, questionId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa câu hỏi khỏi bài tập thành công!");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/assignment/{assignmentId}/batch")
    @PreAuthorize("hasAuthority('LMS_ASSIGNMENT_UPDATE')")
    @LogActivity(module = "LMS", action = "UPDATE", targetType = "assignment_question", description = "Cập nhật hàng loạt danh sách câu hỏi trong bài tập")
    public ResponseEntity<Map<String, Object>> batchReplaceAssignmentQuestions(
            @PathVariable Long assignmentId,
            @RequestBody List<AssignmentQuestionDto> dtos) {

        List<AssignmentQuestionDto> updatedList = assignmentQuestionService.batchReplaceAssignmentQuestions(assignmentId, dtos);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Cập nhật danh sách câu hỏi trong bài tập thành công!");
        response.put("data", updatedList);

        return ResponseEntity.ok(response);
    }
}
