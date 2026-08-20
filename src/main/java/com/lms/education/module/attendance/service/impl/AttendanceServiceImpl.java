package com.lms.education.module.attendance.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.attendance.dto.AttendanceDto;
import com.lms.education.module.attendance.entity.Attendance;
import com.lms.education.module.attendance.repository.AttendanceRepository;
import com.lms.education.module.attendance.service.AttendanceService;
import com.lms.education.module.enrollment.entity.Enrollment;
import com.lms.education.module.enrollment.repository.EnrollmentRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceSheetByScheduleAndDate(Long scheduleId, LocalDate attendanceDate) {
        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId));

        validateScheduleDate(schedule, attendanceDate);

        if (schedule.getClasses() == null) {
            throw new ResourceNotFoundException("Lịch học ID " + scheduleId + " chưa được liên kết với lớp học nào!");
        }


        List<Enrollment> enrollments = enrollmentRepository.findByClassesId(schedule.getClasses().getId());
        List<Attendance> savedAttendances = attendanceRepository.findByScheduleIdAndAttendanceDate(scheduleId, attendanceDate);

        Map<Long, Attendance> attendanceMap = savedAttendances.stream()
                .filter(a -> a.getStudent() != null)
                .collect(Collectors.toMap(a -> a.getStudent().getId(), a -> a, (a1, a2) -> a1));

        List<AttendanceDto> sheet = new ArrayList<>();
        for (Enrollment e : enrollments) {
            if (e.getStatus() != null && !"ACTIVE".equalsIgnoreCase(e.getStatus())) {
                continue; // Only active enrolled students
            }
            Student student = e.getStudent();
            if (student == null) continue;

            Attendance saved = attendanceMap.get(student.getId());
            if (saved != null) {
                sheet.add(toDto(saved));
            } else {
                // Default unmarked student to null status
                sheet.add(AttendanceDto.builder()
                        .scheduleId(scheduleId)
                        .studentId(student.getId())
                        .attendanceDate(attendanceDate)
                        .status(null)
                        .note("")
                        .studentName(student.getFullName())
                        .studentCode(student.getStudentCode())
                        .className(schedule.getClasses().getName())
                        .courseName(schedule.getClasses().getCourse() != null ? schedule.getClasses().getCourse().getName() : null)
                        .dayOfWeek(formatDayOfWeek(schedule.getDayOfWeek()))
                        .timeSlot(schedule.getStartTime() != null && schedule.getEndTime() != null

                                ? schedule.getStartTime() + " - " + schedule.getEndTime() : null)
                        .build());
            }
        }
        return sheet;
    }

    @Override
    @Transactional
    public AttendanceDto markAttendance(AttendanceDto dto) {
        Attendance saved = upsertAttendanceInternal(dto);
        return toDto(saved);
    }

    @Override
    @Transactional
    public List<AttendanceDto> batchMarkAttendance(Long scheduleId, LocalDate attendanceDate, List<AttendanceDto> dtos) {
        if (dtos != null) {
            for (AttendanceDto dto : dtos) {
                if (dto.getStudentId() == null) continue;

                if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
                    attendanceRepository.findByScheduleIdAndStudentIdAndAttendanceDate(scheduleId, dto.getStudentId(), attendanceDate)
                            .ifPresent(attendanceRepository::delete);
                    continue;
                }

                dto.setScheduleId(scheduleId);
                dto.setAttendanceDate(attendanceDate);
                upsertAttendanceInternal(dto);
            }
        }
        return getAttendanceSheetByScheduleAndDate(scheduleId, attendanceDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceDto> getAttendanceByStudent(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Không tìm thấy học viên với ID: " + studentId);
        }
        return attendanceRepository.findByStudentId(studentId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi điểm danh với ID: " + id));
        attendanceRepository.delete(attendance);
    }

    private Attendance upsertAttendanceInternal(AttendanceDto dto) {
        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + dto.getScheduleId()));

        validateScheduleDate(schedule, dto.getAttendanceDate());

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + dto.getStudentId()));

        Attendance attendance = attendanceRepository.findByScheduleIdAndStudentIdAndAttendanceDate(
                dto.getScheduleId(), dto.getStudentId(), dto.getAttendanceDate()
        ).orElse(Attendance.builder()
                .schedule(schedule)
                .student(student)
                .attendanceDate(dto.getAttendanceDate())
                .build());

        attendance.setStatus(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "PRESENT");
        attendance.setNote(dto.getNote());

        return attendanceRepository.save(attendance);
    }

    private AttendanceDto toDto(Attendance entity) {
        ClassSchedule schedule = entity.getSchedule();
        Student student = entity.getStudent();
        return AttendanceDto.builder()
                .id(entity.getId())
                .scheduleId(schedule != null ? schedule.getId() : null)
                .studentId(student != null ? student.getId() : null)
                .attendanceDate(entity.getAttendanceDate())
                .status(entity.getStatus())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .studentName(student != null ? student.getFullName() : null)
                .studentCode(student != null ? student.getStudentCode() : null)
                .className(schedule != null && schedule.getClasses() != null ? schedule.getClasses().getName() : null)
                .courseName(schedule != null && schedule.getClasses() != null && schedule.getClasses().getCourse() != null
                        ? schedule.getClasses().getCourse().getName() : null)
                .dayOfWeek(schedule != null ? formatDayOfWeek(schedule.getDayOfWeek()) : null)
                .timeSlot(schedule != null && schedule.getStartTime() != null && schedule.getEndTime() != null
                        ? schedule.getStartTime() + " - " + schedule.getEndTime() : null)
                .build();
    }

    private void validateScheduleDate(ClassSchedule schedule, LocalDate date) {
        if (schedule.getDayOfWeek() != null && date != null) {
            int dateDow = (date.getDayOfWeek().getValue() == 7) ? 8 : (date.getDayOfWeek().getValue() + 1);
            if (schedule.getDayOfWeek() != dateDow) {
                throw new ResourceNotFoundException("Không tìm thấy buổi học nào cho lịch học ID " + schedule.getId() +
                        " vào ngày " + date + " (lịch học cố định diễn ra vào " + formatDayOfWeek(schedule.getDayOfWeek()) + ")!");
            }
        }
    }

    private String formatDayOfWeek(Integer dow) {
        if (dow == null) return null;
        if (dow == 8) return "Chủ nhật";
        return "Thứ " + dow;
    }
}


