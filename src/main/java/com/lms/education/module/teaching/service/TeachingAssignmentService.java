package com.lms.education.module.teaching.service;

import com.lms.education.module.teaching.dto.TeachingAssignmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TeachingAssignmentService {

    TeachingAssignmentDto create(TeachingAssignmentDto dto);

    TeachingAssignmentDto update(Long id, TeachingAssignmentDto dto);

    void delete(Long id);

    TeachingAssignmentDto getById(Long id);

    Page<TeachingAssignmentDto> getAllAssignments(String keyword, Pageable pageable);

    List<TeachingAssignmentDto> getAssignmentsByClassId(Long classId);
}
