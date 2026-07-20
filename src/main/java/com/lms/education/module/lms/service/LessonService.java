package com.lms.education.module.lms.service;

import com.lms.education.module.lms.dto.LessonDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LessonService {

    LessonDto create(LessonDto dto);

    LessonDto update(Long id, LessonDto dto);

    void delete(Long id);

    LessonDto getById(Long id);

    Page<LessonDto> getAll(String keyword, Pageable pageable);

    List<LessonDto> getByClassId(Long classId);
}
