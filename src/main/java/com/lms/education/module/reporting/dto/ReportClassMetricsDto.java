package com.lms.education.module.reporting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportClassMetricsDto {

    private Long classId;
    private String classCode;
    private String className;

    @Builder.Default
    private Integer totalStudents = 0;

    @Builder.Default
    private BigDecimal averageAttendanceRate = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal averageAssignmentScore = BigDecimal.ZERO;

    @Builder.Default
    private Integer droppedStudents = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastCalculatedAt;

    // Các trường thông tin bổ sung khi lọc theo giai đoạn cụ thể (cho Bộ phận Đào tạo)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
