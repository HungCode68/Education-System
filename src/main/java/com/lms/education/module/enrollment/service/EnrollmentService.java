package com.lms.education.module.enrollment.service;

import com.lms.education.module.enrollment.dto.BulkEnrollmentDto;
import com.lms.education.module.enrollment.dto.EnrollmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface EnrollmentService {

    EnrollmentDto create(EnrollmentDto dto);

    EnrollmentDto update(Long id, EnrollmentDto dto);

    void delete(Long id);

    EnrollmentDto getById(Long id);

    Page<EnrollmentDto> getAll(String keyword, Pageable pageable);

    List<EnrollmentDto> getByStudentId(Long studentId);

    List<EnrollmentDto> getByClassId(Long classId);

    Map<String, Object> enrollBulk(BulkEnrollmentDto dto);

    void handleStudentStatusCascade(Long studentId, String globalStatus);
}
