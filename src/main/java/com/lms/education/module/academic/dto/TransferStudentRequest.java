package com.lms.education.module.academic.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TransferStudentRequest {
    @NotBlank(message = "Vui lòng chọn học sinh")
    private String studentId;

    @NotBlank(message = "Vui lòng chọn lớp mới")
    private String toClassId;

    private String reason; // Lý do chuyển (Không bắt buộc)
}