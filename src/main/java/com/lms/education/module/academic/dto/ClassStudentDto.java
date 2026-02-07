package com.lms.education.module.academic.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.academic.entity.ClassStudent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class ClassStudentDto {

    private String id;

    // --- Thông tin Lớp học (Input & Output) ---
    @NotBlank(message = "Vui lòng chọn Lớp học")
    private String physicalClassId;

    // Output: Tên lớp để hiển thị (VD: 10A1)
    private String physicalClassName;

    // --- Thông tin Học sinh (Input & Output) ---
    @NotBlank(message = "Vui lòng chọn Học sinh")
    private String studentId;

    // Output: Các thông tin cơ bản của học sinh để hiển thị lên danh sách
    private String studentName; // Họ tên
    private String studentCode; // Mã học sinh (MSHS)
    // Bạn có thể thêm: ngày sinh, giới tính... nếu cần hiển thị luôn trên danh sách lớp

    // --- Thông tin Xếp lớp ---

    // Số thứ tự (STT) trong sổ điểm
    @Min(value = 1, message = "Số thứ tự phải lớn hơn 0")
    private Integer studentNumber;

    // Ngày vào lớp (Mặc định là ngày hiện tại nếu không gửi)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate enrollmentDate;

    // Ngày kết thúc (Khi chuyển lớp/thôi học)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Trạng thái (Đang học, Đã chuyển...)
    private ClassStudent.StudentStatus status;

    // --- Timestamps ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}