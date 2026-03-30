package com.lms.education.module.assignments.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.assignments.entity.AssignmentSubmission;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionDto {

    private String id;

    @NotBlank(message = "ID bài tập không được để trống")
    private String assignmentId;
    private String assignmentTitle; // Lấy thêm tên bài tập để hiển thị cho đẹp

    // Nếu học sinh tự nộp, hệ thống sẽ tự lấy ID từ Token,
    private String studentId;
    private String studentName;
    private String studentCode;

    private String studentNote;

    private AssignmentSubmission.SubmissionStatus submissionStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime submittedAt;

    private Boolean isLate;
    private Integer attemptCount;
    private Double score;
    private String teacherFeedback;

    private String gradedById;
    private String gradedByName; // Tên giáo viên chấm bài

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime gradedAt;

    private AssignmentSubmission.GradingMethod gradingMethod;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
