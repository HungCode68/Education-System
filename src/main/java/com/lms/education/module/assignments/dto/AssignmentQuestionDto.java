package com.lms.education.module.assignments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.assignments.entity.AssignmentQuestion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentQuestionDto {

    private String id;

    @NotBlank(message = "ID bài tập (assignmentId) không được để trống")
    private String assignmentId;

    @NotNull(message = "Thứ tự câu hỏi không được để trống")
    private Integer questionOrder;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    private String explanation;

    @NotNull(message = "Loại câu hỏi không được để trống")
    private AssignmentQuestion.QuestionType questionType;

    @PositiveOrZero(message = "Điểm số không được là số âm")
    private Double score;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
