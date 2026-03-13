package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeRoleRequest {
    @NotBlank(message = "Mã quyền (Role Code) không được để trống")
    private String roleCode; // Ví dụ: "ADMIN", "SUBJECT_TEACHER"
}
