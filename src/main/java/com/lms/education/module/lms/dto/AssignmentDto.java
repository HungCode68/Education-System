package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentDto {

    private Long id;

    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    private String lessonName;
    private Long classId;
    private String classCode;
    private String className;

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Hạn nộp bài (dueDate) không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    private String assignmentType; // HOMEWORK, ESSAY, QUIZ, PROJECT

    private Integer timeLimitMinutes; // Phút

    private Integer maxAttempts;

    private Boolean showCorrectAnswers;

    private String status; // UNPUBLISHED, PUBLISHED, CLOSED

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
