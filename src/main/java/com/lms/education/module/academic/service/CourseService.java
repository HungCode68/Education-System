package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.CourseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseService {

    CourseDto create(CourseDto dto);

    CourseDto update(Long id, CourseDto dto);

    void delete(Long id);

    CourseDto getById(Long id);

    CourseDto getByCode(String code);

    Page<CourseDto> getAllCourses(String keyword, Pageable pageable);
}