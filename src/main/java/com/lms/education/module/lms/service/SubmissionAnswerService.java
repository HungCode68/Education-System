package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.SubmissionAnswerDto;

import java.util.List;

public interface SubmissionAnswerService {

    List<SubmissionAnswerDto> getAnswersBySubmissionId(Long submissionId);

    SubmissionAnswerDto saveOrUpdateAnswer(Long submissionId, SubmissionAnswerDto dto);

    List<SubmissionAnswerDto> batchSaveAnswers(Long submissionId, List<SubmissionAnswerDto> dtos);

    void removeAnswer(Long submissionId, Long questionId);

    SubmissionAnswerDto gradeAnswer(Long answerId, java.math.BigDecimal score);

    List<SubmissionAnswerDto> batchGradeAnswers(Long submissionId, List<com.lms.education.module.lms.dto.GradeAnswerDto> grades);
}
