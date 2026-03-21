package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignPermissionDto {

    @NotBlank(message = "ID của Vai trò (Role) không được để trống")
    private String roleId;

    // Danh sách các ID của Permission mà Frontend gửi lên (Tương ứng với các checkbox được tích)
    @NotEmpty(message = "Danh sách quyền không được để trống")
    private Set<Integer> permissionIds;
}
