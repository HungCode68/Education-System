package com.lms.education.module.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatSessionDto {
    private Long id;
    private String title;
    private String status;
    private LocalDateTime updatedAt;
}
