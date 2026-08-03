package com.lms.education.module.teaching.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScheduleAssignmentDto {

    private Long id;

    @NotNull(message = "ID lịch học không được để trống")
    private Long scheduleId;

    private Integer dayOfWeek;
    private String startTime;
    private String endTime;
    private String roomName;

    private String classCode;
    private String className;

    @NotNull(message = "ID giảng viên không được để trống")
    private Long staffId;

    private String staffCode;
    private String teacherName;

    @Size(max = 50, message = "Vai trò không vượt quá 50 ký tự")
    private String role; // MAIN_TEACHER, NATIVE_TEACHER, ASSISTANT

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
