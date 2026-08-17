package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.ScheduleCancellationDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ScheduleCancellationService {
    ScheduleCancellationDto create(ScheduleCancellationDto dto);
    ScheduleCancellationDto update(Long id, ScheduleCancellationDto dto);
    void delete(Long id);
    ScheduleCancellationDto getById(Long id);
    Page<ScheduleCancellationDto> getAll(Long classId, Pageable pageable);
}
