package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.AssignmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AssignmentService {

    AssignmentDto create(AssignmentDto dto);

    AssignmentDto update(Long id, AssignmentDto dto);

    void delete(Long id);

    AssignmentDto getById(Long id);

    List<AssignmentDto> getByLessonId(Long lessonId);

    List<AssignmentDto> getByClassId(Long classId);

    Page<AssignmentDto> getAll(String keyword, Pageable pageable);
}
