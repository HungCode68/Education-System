package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.SubmissionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface SubmissionService {

    SubmissionDto startSubmission(Long assignmentId);

    SubmissionDto submitAssignment(Long submissionId);

    SubmissionDto gradeSubmission(Long submissionId, BigDecimal score, String feedback);

    SubmissionDto getById(Long id);

    SubmissionDto getMySubmission(Long assignmentId);

    List<SubmissionDto> getMySubmissionHistory(Long assignmentId);

    List<SubmissionDto> getMySubmissionsByClassId(Long classId);

    List<SubmissionDto> getByAssignmentId(Long assignmentId);

    List<SubmissionDto> getByStudentId(Long studentId);

    Page<SubmissionDto> getByAssignmentIdPageable(Long assignmentId, String status, String keyword, Pageable pageable);
}
