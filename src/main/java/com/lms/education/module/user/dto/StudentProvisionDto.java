package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class StudentProvisionDto {

    @NotEmpty(message = "Danh sách học viên không được để trống")
    private List<Long> studentIds;

    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private List<Long> roleIds;
}