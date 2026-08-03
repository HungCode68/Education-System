package com.lms.education.module.reporting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
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
public class ReportCenterStatisticsDto {

    @NotNull(message = "Ngày thống kê báo cáo không được để trống")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate reportDate;

    @Builder.Default
    private Integer totalActiveStudents = 0;

    @Builder.Default
    private Integer newStudentsToday = 0;

    @Builder.Default
    private Integer droppedStudentsToday = 0;

    @Builder.Default
    private Integer totalTeachers = 0;

    @Builder.Default
    private Integer newTeachersToday = 0;

    @Builder.Default
    private Integer resignedTeachersToday = 0;

    @Builder.Default
    private Integer totalOtherStaffs = 0;

    @Builder.Default
    private Integer newStaffsToday = 0;

    @Builder.Default
    private Integer resignedStaffsToday = 0;

    @Builder.Default
    private Integer totalCourses = 0;

    @Builder.Default
    private Integer totalActiveClasses = 0;

    @Builder.Default
    private Integer newClassesOpened = 0;

    @Builder.Default
    private Integer classesClosedToday = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
