package com.lms.education.module.academic.dto;

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
public class ClassTransferHistoryDto {

    private String id;

    // --- Thông tin Học sinh ---
    private String studentId;
    private String studentName; // Họ tên
    private String studentCode; // Mã học sinh (Để dễ tra cứu)

    // --- Thông tin Lớp cũ ---
    private String fromClassId;
    private String fromClassName; // VD: 10A1

    // --- Thông tin Lớp mới ---
    private String toClassId;
    private String toClassName;   // VD: 10A2

    // --- Chi tiết chuyển ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate transferDate;

    private String reason;

    // --- Audit (Người thực hiện) ---
    private String createdById;
    private String createdByName; // Tên cán bộ thực hiện chuyển lớp

    // --- Timestamps ---
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}