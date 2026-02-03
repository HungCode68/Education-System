package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.academic.entity.GradeSubject;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GradeSubjectDto {

    private String id;

    // --- Input: Bắt buộc phải có ID của 2 bảng cha để liên kết ---
    @NotBlank(message = "Vui lòng chọn Khối")
    private String gradeId;

    @NotBlank(message = "Vui lòng chọn Môn học")
    private String subjectId;

    // --- Output: Kèm thêm Tên để hiển thị  ---
    private String gradeName;
    private String subjectName;

    // --- Các thuộc tính cấu hình ---

    // Loại môn (Bắt buộc hoặc Tự chọn)
    private GradeSubject.SubjectType subjectType;

    // Cờ bật tắt LMS
    private Boolean isLmsEnabled;

    // Thứ tự hiển thị
    @Min(value = 0, message = "Thứ tự hiển thị không được âm")
    private Integer displayOrder;

    // --- Timestamps ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
