package com.lms.education.module.lms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeAnswerDto {
    @NotNull(message = "ID câu trả lời không được để trống")
    private Long answerId;
    
    private BigDecimal score;
}
