package com.lms.education.module.teaching.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeachingSubstitutionDto {

    private Long id;

    @NotNull(message = "ID lịch học không được để trống")
    private Long scheduleId;

    private String classCode;
    private String className;

    @NotNull(message = "ID giáo viên vắng mặt không được để trống")
    private Long absentStaffId;

    private String absentStaffName;
    private String absentStaffCode;

    @NotNull(message = "ID giáo viên dạy thay không được để trống")
    private Long substituteStaffId;

    private String substituteStaffName;
    private String substituteStaffCode;

    @NotNull(message = "Ngày bắt đầu dạy thay không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc dạy thay không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @NotBlank(message = "Lý do dạy thay không được để trống")
    private String reason;

    @Size(max = 20, message = "Trạng thái không vượt quá 20 ký tự")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
