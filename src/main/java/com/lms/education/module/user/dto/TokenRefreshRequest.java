package com.lms.education.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRefreshRequest {

    @NotBlank(message = "Refresh Token không được để trống")
    private String refreshToken;

}