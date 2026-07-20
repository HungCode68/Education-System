package com.lms.education.module.teaching.service;

import com.lms.education.module.teaching.dto.ScheduleAssignmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScheduleAssignmentService {

    ScheduleAssignmentDto create(ScheduleAssignmentDto dto);

    ScheduleAssignmentDto update(Long id, ScheduleAssignmentDto dto);

    void delete(Long id);

    ScheduleAssignmentDto getById(Long id);

    Page<ScheduleAssignmentDto> getAll(String keyword, Pageable pageable);

    List<ScheduleAssignmentDto> getAssignmentsByClassId(Long classId);

    List<ScheduleAssignmentDto> getAssignmentsByScheduleId(Long scheduleId);
}
