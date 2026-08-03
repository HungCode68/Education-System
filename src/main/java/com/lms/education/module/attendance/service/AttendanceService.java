package com.lms.education.module.attendance.service;

import com.lms.education.module.attendance.dto.AttendanceDto;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    List<AttendanceDto> getAttendanceSheetByScheduleAndDate(Long scheduleId, LocalDate attendanceDate);

    AttendanceDto markAttendance(AttendanceDto dto);

    List<AttendanceDto> batchMarkAttendance(Long scheduleId, LocalDate attendanceDate, List<AttendanceDto> dtos);

    List<AttendanceDto> getAttendanceByStudent(Long studentId);

    void deleteAttendance(Long id);
}
