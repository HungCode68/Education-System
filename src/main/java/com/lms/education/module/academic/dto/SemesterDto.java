package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.academic.entity.Semester;
import jakarta.validation.constraints.Min;
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
public class SemesterDto {

    private String id;

    @NotBlank(message = "Tên học kỳ không được để trống")
    private String name; // VD: Học kỳ 1

    @NotBlank(message = "Mã học kỳ không được để trống")
    private String code; // VD: HK1

    @NotNull(message = "Thứ tự học kỳ không được để trống")
    @Min(value = 1, message = "Thứ tự phải lớn hơn hoặc bằng 1")
    private Integer priority; // 1, 2, 3

    // --- Liên kết Năm học ---
    @NotBlank(message = "Vui lòng chọn Năm học")
    private String schoolYearId;

    // Output: Tên năm học để hiển thị (VD: 2025-2026)
    private String schoolYearName;

    // --- Thời gian ---
    @NotNull(message = "Ngày bắt đầu không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // --- Trạng thái ---
    // Sử dụng Enum định nghĩa sẵn trong Entity
    private Semester.SemesterStatus status;

    // --- Audit ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}