package com.lms.education.module.teaching.service;

import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeachingSubstitutionService {

    TeachingSubstitutionDto create(TeachingSubstitutionDto dto);

    TeachingSubstitutionDto update(Long id, TeachingSubstitutionDto dto);

    void delete(Long id);

    TeachingSubstitutionDto getById(Long id);

    Page<TeachingSubstitutionDto> getAll(String keyword, Pageable pageable);

    List<TeachingSubstitutionDto> getSubstitutionsByClassId(Long classId);
}
