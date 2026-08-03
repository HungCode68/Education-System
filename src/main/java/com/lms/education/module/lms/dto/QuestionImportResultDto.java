package com.lms.education.module.lms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionImportResultDto {

    private boolean success;
    private int totalRows;
    private int successCount;
    private int errorCount;

    @Builder.Default
    private List<QuestionImportErrorDto> errors = new ArrayList<>();

    @Builder.Default
    private List<QuestionDto> importedQuestions = new ArrayList<>();
}
