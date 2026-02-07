package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.academic.entity.PhysicalClass;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class PhysicalClassDto {

    private String id;

    // --- Thông tin cơ bản ---
    @NotBlank(message = "Tên lớp không được để trống")
    @Size(max = 100, message = "Tên lớp tối đa 100 ký tự")
    private String name; // VD: 10A1

    @Size(max = 50, message = "Số phòng tối đa 50 ký tự")
    private String roomNumber; // VD: B102

    @NotNull(message = "Sĩ số tối đa không được để trống")
    @Min(value = 1, message = "Sĩ số phải lớn hơn 0")
    private Integer maxStudents;

    // --- Quan hệ Năm học (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Năm học")
    private String schoolYearId;

    // Output: Tên năm học (để hiển thị)
    private String schoolYearName;

    // --- Quan hệ Khối (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Khối")
    private String gradeId;

    // Output: Tên khối (để hiển thị)
    private String gradeName;

    // --- Quan hệ Giáo viên chủ nhiệm (Có thể để trống lúc tạo) ---
    private String homeroomTeacherId;

    // Output: Tên và Mã giáo viên (để hiển thị rõ ràng)
    private String homeroomTeacherName;
    private String homeroomTeacherCode;

    // --- Trạng thái ---
    private PhysicalClass.ClassStatus status;

    // --- Timestamps ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
