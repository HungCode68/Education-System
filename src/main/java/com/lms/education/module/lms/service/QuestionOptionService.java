package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.QuestionOptionDto;

import java.util.List;

public interface QuestionOptionService {

    List<QuestionOptionDto> getByQuestionId(Long questionId);

    QuestionOptionDto create(Long questionId, QuestionOptionDto dto);

    QuestionOptionDto update(Long id, QuestionOptionDto dto);

    void delete(Long id);

    List<QuestionOptionDto> replaceAllForQuestion(Long questionId, List<QuestionOptionDto> dtos);
}
