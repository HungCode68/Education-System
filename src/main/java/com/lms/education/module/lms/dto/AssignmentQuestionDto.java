package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssignmentQuestionDto {

    private Long assignmentId;

    @NotNull(message = "ID câu hỏi không được để trống")
    private Long questionId;


    private Integer orderNumber;

    private BigDecimal scoreWeight;

    
    private Boolean allowMultipleAnswers;

    private QuestionDto question;
}
