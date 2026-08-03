package com.lms.education.module.teaching.service;

import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import com.lms.education.module.user.dto.StaffDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TeachingSubstitutionService {

    TeachingSubstitutionDto create(TeachingSubstitutionDto dto);

    TeachingSubstitutionDto update(Long id, TeachingSubstitutionDto dto);

    void delete(Long id);

    TeachingSubstitutionDto getById(Long id);

    Page<TeachingSubstitutionDto> getAll(String keyword, Pageable pageable);

    List<TeachingSubstitutionDto> getSubstitutionsByClassId(Long classId);

    List<StaffDto> getAvailableTeachers(Long scheduleId, LocalDate startDate, LocalDate endDate, Long excludeSubstitutionId);
}
