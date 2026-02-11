package com.lms.education.module.lms_class.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.lms_class.entity.OnlineClassStudent;
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
public class OnlineClassStudentDto {

    private String id;

    // --- INPUT: Dùng khi thêm học sinh vào lớp ---
    @NotBlank(message = "Vui lòng chọn Lớp học trực tuyến")
    private String onlineClassId;

    @NotBlank(message = "Vui lòng chọn Học sinh")
    private String studentId;

    // --- OUTPUT: Thông tin hiển thị (Flatten Data) ---

    // Thông tin Học sinh (Để hiển thị danh sách thành viên)
    private String studentCode;      // Mã SV (VD: HS001)
    private String studentName;      // Tên SV (VD: Nguyễn Văn A)
    private String studentEmail;     // Email (Nếu cần liên lạc)

    // Thông tin Lớp
    private String onlineClassName;

    // --- TRẠNG THÁI GHI DANH ---

    private OnlineClassStudent.EnrollmentSource enrollmentSource; // system, manual

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate enrolledDate;

    private OnlineClassStudent.StudentStatus status; // active, removed

    // --- AUDIT ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
