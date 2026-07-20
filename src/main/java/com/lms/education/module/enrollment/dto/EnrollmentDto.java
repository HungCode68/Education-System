package com.lms.education.module.enrollment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentDto {

    private Long id;

    @NotNull(message = "ID học viên không được để trống")
    private Long studentId;

    private String studentName;
    private String studentCode;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    private String classCode;
    private String className;

    @NotNull(message = "Ngày nhập học không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrollmentDate;

    private String status; // PENDING, ACTIVE, DROPPED, COMPLETED

    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
