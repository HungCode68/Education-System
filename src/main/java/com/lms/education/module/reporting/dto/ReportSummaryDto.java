package com.lms.education.module.reporting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    // Số liệu tổng quan / hiện trạng đang hoạt động
    @Builder.Default
    private Integer totalActiveStudents = 0;

    @Builder.Default
    private Integer totalTeachers = 0;

    @Builder.Default
    private Integer totalOtherStaffs = 0;

    @Builder.Default
    private Integer totalCourses = 0;

    @Builder.Default
    private Integer totalActiveClasses = 0;

    // Số liệu biến động cộng dồn trong khoảng thời gian [startDate, endDate]
    @Builder.Default
    private Integer totalNewStudentsInRange = 0;

    @Builder.Default
    private Integer totalDroppedStudentsInRange = 0;

    @Builder.Default
    private Integer totalNewTeachersInRange = 0;

    @Builder.Default
    private Integer totalResignedTeachersInRange = 0;

    @Builder.Default
    private Integer totalNewStaffsInRange = 0;

    @Builder.Default
    private Integer totalResignedStaffsInRange = 0;

    @Builder.Default
    private Integer totalNewClassesOpenedInRange = 0;

    @Builder.Default
    private Integer totalClassesClosedInRange = 0;

    @Builder.Default
    private Integer totalDaysReported = 0;
}
