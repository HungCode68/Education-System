package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AccountProvisionDto {

    @NotEmpty(message = "Danh sách nhân sự không được để trống")
    private List<Long> staffIds;

    @NotEmpty(message = "Danh sách vai trò không được để trống")
    private List<Long> roleIds;
}