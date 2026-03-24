package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lms.education.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;

    private String email;

    private User.UserStatus status;

    // Chỉ trả về thông tin Role cần thiết thay vì cả cục Object
    private String roleId;
    private String roleCode;
    private String roleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLogin;
}
