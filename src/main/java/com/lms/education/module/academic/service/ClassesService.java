package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.ClassesDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassesService {

    ClassesDto create(ClassesDto dto);

    ClassesDto update(Long id, ClassesDto dto);

    void delete(Long id);

    ClassesDto getById(Long id);

    Page<ClassesDto> getAllClasses(String keyword, Pageable pageable);

    java.util.List<ClassesDto> getMyClasses();
}
