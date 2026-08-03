package com.lms.education.module.attendance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDto {

    private Long id;

    @NotNull(message = "ID lịch học (scheduleId) không được để trống")
    private Long scheduleId;

    @NotNull(message = "ID học viên (studentId) không được để trống")
    private Long studentId;

    @NotNull(message = "Ngày điểm danh không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    @Pattern(regexp = "PRESENT|ABSENT|EXCUSED|LATE", message = "Trạng thái điểm danh phải là: PRESENT, ABSENT, EXCUSED, hoặc LATE")
    @Builder.Default
    private String status = "PRESENT";

    private String note;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    // Rich display fields
    private String studentName;
    private String studentCode;
    private String className;
    private String courseName;
    private String dayOfWeek;
    private String timeSlot;
}
