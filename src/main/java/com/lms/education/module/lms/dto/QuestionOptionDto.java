package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionOptionDto {

    private Long id;

    private Long questionId;

    @NotBlank(message = "Nội dung lựa chọn không được để trống")
    private String optionContent;

    private Boolean isCorrect;
}
