package com.lms.education.module.teaching.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeachingAssignmentDto {

    private Long id;

    @NotNull(message = "ID nhân viên giảng dạy không được để trống")
    private Long staffId;

    private String staffCode;
    private String teacherName;

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    private String classCode;
    private String className;

    @Size(max = 50, message = "Vai trò không vượt quá 50 ký tự")
    private String role; // MAIN_TEACHER, ASSISTANT_TEACHER, TUTOR

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate assignedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    private String status; // ACTIVE, INACTIVE

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
