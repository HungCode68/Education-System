package com.lms.education.module.lms_class.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class OnlineClassDto {
    private String id;
    private String name; // VD: Toán Học - 10A1
    private String status;

    // --- Thông tin từ Phân công ---
    private String teachingAssignmentId;

    private String subjectId;
    private String subjectName;

    private String physicalClassId;
    private String physicalClassName;

    private String teacherId;
    private String teacherName;
    private String teacherCode;

    private boolean isSubstituted;

    // Thông tin người đang dạy thay
    private String subTeacherId;       // ID Sub teacher (nếu có)
    private String subTeacherName;     // Tên Sub teacher

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate subEndDate;      // Dạy đến ngày nào?

    // --- Meta ---
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
