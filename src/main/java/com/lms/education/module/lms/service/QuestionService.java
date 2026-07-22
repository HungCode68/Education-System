package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.QuestionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionService {

    QuestionDto create(QuestionDto dto, MultipartFile mediaFile);

    QuestionDto update(Long id, QuestionDto dto, MultipartFile mediaFile);

    void delete(Long id);

    QuestionDto getById(Long id);

    List<QuestionDto> getByAssignmentId(Long assignmentId);

    Page<QuestionDto> getAll(String keyword, String questionType, Pageable pageable);
}
