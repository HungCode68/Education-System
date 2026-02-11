package com.lms.education.module.teaching_assignment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignment;
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
public class TeachingAssignmentDto {

    private String id;

    // --- Thông tin Lớp học (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Lớp học")
    private String physicalClassId;

    // Output: Tên lớp để hiển thị (VD: 10A1)
    private String physicalClassName;

    // --- Thông tin Môn học (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Môn học")
    private String subjectId;

    // Output: Tên môn (VD: Toán Học)
    private String subjectName;
    private String subjectCode;

    // --- Thông tin Giáo viên (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Giáo viên")
    private String teacherId;

    // Output: Tên GV (VD: Nguyễn Văn A)
    private String teacherName;
    private String teacherCode;

    // --- Thông tin Thời gian (Bắt buộc) ---
    @NotBlank(message = "Vui lòng chọn Năm học")
    private String schoolYearId;
    private String schoolYearName; // Output

    @NotBlank(message = "Vui lòng chọn Học kỳ")
    private String semesterId;
    private String semesterName;
    // Nếu bạn có bảng Semester riêng thì thêm semesterName vào đây

    // --- Thông tin bổ sung ---
    private String onlineClassId; // Có thể null (nếu chỉ dạy offline)

    private TeachingAssignment.AssignmentStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate assignedDate; // Ngày bắt đầu phân công

    private boolean isSubstituted;

    // Nếu có, ai là người đang dạy thay HÔM NAY?
    private String subTeacherId;
    private String subTeacherName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate subStartDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate subEndDate;

    // --- Timestamps ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
