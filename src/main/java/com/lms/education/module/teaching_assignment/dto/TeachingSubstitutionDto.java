package com.lms.education.module.teaching_assignment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeachingSubstitutionDto {

    private String id;

    // --- INPUT: Thông tin cần thiết để tạo ---
    @NotBlank(message = "Vui lòng chọn phân công gốc cần dạy thay")
    private String assignmentId;

    @NotBlank(message = "Vui lòng chọn giáo viên dạy thay")
    private String subTeacherId;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String reason;
    private TeachingSubstitution.SubstitutionStatus status;

    // --- OUTPUT: Thông tin hiển thị ---
    // Giúp UI hiển thị đầy đủ ngữ cảnh mà không cần gọi nhiều API

    // Dạy thay cho ai?
    private String originalTeacherId;
    private String originalTeacherName;
    private String originalTeacherCode;

    // Người dạy thay là ai?
    private String subTeacherName;
    private String subTeacherCode;

    // Dạy lớp nào, môn gì? (Lấy từ Assignment gốc)
    private String physicalClassId;
    private String physicalClassName; // VD: 10A1

    private String subjectId;
    private String subjectName;       // VD: Toán Học

    // --- Audit ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
