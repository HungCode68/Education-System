package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmissionAnswerDto {

    private Long id;

    private Long submissionId;

    @NotNull(message = "ID câu hỏi không được để trống")
    private Long questionId;

    private Long selectedOptionId;

    private String textAnswer;

    private BigDecimal earnedScore;

    private Boolean isAutoGraded;

    // Rich display fields
    private String questionContent;
    private String questionType;
    private String selectedOptionContent;
    private Boolean isSelectedOptionCorrect;
}
