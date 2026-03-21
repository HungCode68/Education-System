package com.lms.education.module.user.dto;

import com.lms.education.module.user.entity.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {

    private Integer id;

    @NotBlank(message = "Mã quyền (code) không được để trống")
    @Size(max = 120, message = "Mã quyền không được vượt quá 120 ký tự")
    private String code;

    @NotNull(message = "Nhóm phạm vi (scope) không được để trống")
    private Permission.PermissionScope scope;

    @Size(max = 120, message = "Tên quyền không được vượt quá 120 ký tự")
    private String name;

    private String description;
}
