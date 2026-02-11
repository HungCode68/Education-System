package com.lms.education.module.teaching_assignment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lms.education.module.teaching_assignment.entity.TeachingAssignmentHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Ẩn các trường null (VD: oldTeacher khi tạo mới)
public class TeachingAssignmentHistoryDto {

    private String id;
    private String assignmentId;

    // --- NGỮ CẢNH (Context) ---
    // Giúp hiển thị rõ ràng khi xem lịch sử tổng hợp toàn trường
    // VD: "Thay đổi giáo viên lớp 10A1 môn Toán"
    private String physicalClassName;
    private String subjectName;

    // --- GIÁO VIÊN CŨ (Trước khi thay đổi) ---
    private String oldTeacherId;
    private String oldTeacherName;
    private String oldTeacherCode;

    // --- GIÁO VIÊN MỚI (Sau khi thay đổi) ---
    private String newTeacherId;
    private String newTeacherName;
    private String newTeacherCode;

    // --- CHI TIẾT HÀNH ĐỘNG ---
    private TeachingAssignmentHistory.ActionType actionType; // ASSIGNED, REPLACED...
    private String reason;

    // --- AUDIT ---
    private String changedBy; // ID người thực hiện

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime changedAt;
}
