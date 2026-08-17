package com.lms.education.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {

    // Optional, if null it means the user is not a student (e.g., Staff/Admin)
    private Long studentId;

    // Optional, if null, a new session is created
    private Long sessionId;

    @NotBlank(message = "Message cannot be blank")
    private String message;
}
