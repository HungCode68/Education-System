package com.lms.education.module.assignments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.assignments.entity.Assignment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDto {

    private String id;

    @NotBlank(message = "ID Lớp học không được để trống")
    private String onlineClassId;

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    private String title;

    private String description;

    private String attachmentPath;

    private String attachmentUrl;

    @NotNull(message = "Loại bài tập không được để trống")
    private Assignment.AssignmentType assignmentType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueTime;

    private Integer durationMinutes;

    @DecimalMin(value = "0.0", message = "Điểm tối đa không được nhỏ hơn 0")
    @DecimalMax(value = "100.0", message = "Điểm tối đa không được vượt quá 100")
    private BigDecimal maxScore;

    private Boolean allowLateSubmission;

    private Integer maxAttempts;

    private Boolean shuffleQuestions;

    private Boolean viewAnswers;

    private Assignment.AssignmentStatus status;

    private String createdById;

    // Thêm tên người tạo và tên lớp để hiển thị
    private String createdByName;
    private String onlineClassName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
