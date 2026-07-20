package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "ID của Vai trò (Role) không được để trống")
    private Long roleId;

    // Danh sách các ID của Permission mà Frontend gửi lên (Tương ứng với các checkbox được tích)
    @NotEmpty(message = "Danh sách quyền không được để trống")
    private Set<Long> permissionIds;
}
