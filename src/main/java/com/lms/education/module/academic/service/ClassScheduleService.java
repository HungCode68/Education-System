package com.lms.education.module.academic.service;

import com.lms.education.module.academic.dto.ClassScheduleDto;
import com.lms.education.module.academic.dto.TimetableEntryDto;

import java.time.LocalDate;
import java.util.List;

public interface ClassScheduleService {

    ClassScheduleDto create(ClassScheduleDto dto);

    ClassScheduleDto update(Long id, ClassScheduleDto dto);

    void delete(Long id);

    ClassScheduleDto getById(Long id);

    List<ClassScheduleDto> getSchedulesByClassId(Long classId);

    List<TimetableEntryDto> getStudentTimetable(Long studentId, LocalDate startDate, LocalDate endDate);

    List<TimetableEntryDto> getTeacherTimetable(Long teacherId, LocalDate startDate, LocalDate endDate);

    List<TimetableEntryDto> getMyTeacherTimetable(LocalDate startDate, LocalDate endDate);

    List<TimetableEntryDto> getTimetable(LocalDate startDate, LocalDate endDate, Long classId);
}
