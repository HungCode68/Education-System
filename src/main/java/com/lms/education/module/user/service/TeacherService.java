package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.TeacherDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeacherService {
    TeacherDto create(TeacherDto dto);
    TeacherDto update(String id, TeacherDto dto);
    void delete(String id);
    TeacherDto getById(String id);
    Page<TeacherDto> getAll(Pageable pageable);
}
