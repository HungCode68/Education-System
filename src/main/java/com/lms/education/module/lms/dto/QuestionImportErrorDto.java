package com.lms.education.module.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportErrorDto {

    private int rowNumber;
    private String columnName;
    private String errorMessage;
}
