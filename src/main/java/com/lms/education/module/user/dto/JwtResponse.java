package com.lms.education.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token; // Access Token

    @Builder.Default
    private String type = "Bearer";

    private String refreshToken; // Token dùng để cấp lại Access Token

    private Long id;

    private String email;

    private String fullName;

    private List<String> roles; // Danh sách quyền của người dùng
}