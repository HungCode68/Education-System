package com.lms.education.module.enrollment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEnrollmentDto {

    @NotNull(message = "ID lớp học không được để trống")
    private Long classId;

    @NotEmpty(message = "Danh sách ID học viên không được để trống")
    private List<Long> studentIds;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate enrollmentDate;

    private String status; // PENDING, ACTIVE

    private String note;
}
