package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.AssignmentQuestionDto;

import java.math.BigDecimal;
import java.util.List;

public interface AssignmentQuestionService {

    List<AssignmentQuestionDto> getByAssignmentId(Long assignmentId);

    AssignmentQuestionDto addQuestionToAssignment(Long assignmentId, AssignmentQuestionDto dto);

    AssignmentQuestionDto updateQuestionInAssignment(Long assignmentId, Long questionId, Integer orderNumber, BigDecimal scoreWeight);

    void removeQuestionFromAssignment(Long assignmentId, Long questionId);

    List<AssignmentQuestionDto> batchReplaceAssignmentQuestions(Long assignmentId, List<AssignmentQuestionDto> dtos);
}
