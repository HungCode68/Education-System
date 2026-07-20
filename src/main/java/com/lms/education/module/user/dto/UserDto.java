package com.lms.education.module.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Chỉ gửi các trường không null về Client
public class UserDto {

    private Long id;

    private String email;

    private String fullName;

    private String phone;

    private String status;

    private Set<String> roles; // Chỉ cần trả về tên Role (VD: ROLE_ADMIN, ROLE_TEACHER)

    private LocalDateTime expiryDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}