package com.lms.education.module.academic.service.impl;

import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.dto.ClassScheduleDto;
import com.lms.education.module.academic.dto.TimetableEntryDto;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.entity.Room;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.academic.repository.RoomRepository;
import com.lms.education.module.academic.service.ClassScheduleService;
import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleServiceImpl implements ClassScheduleService {

    private final ClassScheduleRepository classScheduleRepository;
    private final ClassesRepository classesRepository;
    private final RoomRepository roomRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public ClassScheduleDto create(ClassScheduleDto dto) {
        validateTime(dto);

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        Room room = null;
        if (dto.getRoomId() != null) {
            room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + dto.getRoomId()));
        }

        validateConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), null);

        ClassSchedule classSchedule = ClassSchedule.builder()
                .classes(classes)
                .room(room)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        ClassSchedule saved = classScheduleRepository.save(classSchedule);
        log.info("Đã tạo lịch học cho lớp: {} (Thứ {}, {}-{}) tại phòng: {}",
                classes.getCode(), saved.getDayOfWeek(), saved.getStartTime(), saved.getEndTime(),
                room != null ? room.getName() : "LMS/Online");

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ClassScheduleDto update(Long id, ClassScheduleDto dto) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + id));

        validateTime(dto);

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        Room room = null;
        if (dto.getRoomId() != null) {
            room = roomRepository.findById(dto.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng học với ID: " + dto.getRoomId()));
        }

        validateConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), id);

        classSchedule.setClasses(classes);
        classSchedule.setRoom(room);
        classSchedule.setDayOfWeek(dto.getDayOfWeek());
        classSchedule.setStartTime(dto.getStartTime());
        classSchedule.setEndTime(dto.getEndTime());

        ClassSchedule updated = classScheduleRepository.save(classSchedule);
        log.info("Đã cập nhật lịch học ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + id));

        classScheduleRepository.delete(classSchedule);
        log.info("Đã xóa lịch học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassScheduleDto getById(Long id) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + id));
        return mapToDto(classSchedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassScheduleDto> getSchedulesByClassId(Long classId) {
        if (!classesRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId);
        }
        return classScheduleRepository.findByClassesId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- Helper Methods ---

    private void validateTime(ClassScheduleDto dto) {
        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
            throw new OperationNotPermittedException("Giờ bắt đầu phải trước giờ kết thúc!");
        }
    }

    private void validateConflicts(Long classId, Long roomId, Integer dayOfWeek, java.time.LocalTime start, java.time.LocalTime end, Long excludeId) {
        boolean isClassConflict = classScheduleRepository.existsClassConflict(classId, dayOfWeek, start, end, excludeId);
        if (isClassConflict) {
            throw new OperationNotPermittedException("Lớp học này đã có lịch học trùng với khoảng thời gian được chọn!");
        }

        if (roomId != null) {
            boolean isRoomConflict = classScheduleRepository.existsRoomConflict(roomId, dayOfWeek, start, end, excludeId);
            if (isRoomConflict) {
                throw new OperationNotPermittedException("Phòng học này đã được sử dụng bởi một lớp học khác trong khoảng thời gian này!");
            }
        }
    }

    private ClassScheduleDto mapToDto(ClassSchedule entity) {
        return ClassScheduleDto.builder()
                .id(entity.getId())
                .classId(entity.getClasses().getId())
                .classCode(entity.getClasses().getCode())
                .className(entity.getClasses().getName())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .roomName(entity.getRoom() != null ? entity.getRoom().getName() : "LMS/Online")
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getStudentTimetable(Long studentId, LocalDate startDate, LocalDate endDate) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Không tìm thấy học viên với ID: " + studentId);
        }

        List<ClassSchedule> studentSchedules = classScheduleRepository.findSchedulesByStudentId(studentId);
        List<TimetableEntryDto> timetable = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int systemDayOfWeek = date.getDayOfWeek().getValue() + 1;

            for (ClassSchedule schedule : studentSchedules) {
                if (schedule.getDayOfWeek() == systemDayOfWeek) {
                    List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findByScheduleId(schedule.getId());
                    ScheduleAssignment mainAssignment = assignments.stream()
                            .filter(sa -> "MAIN_TEACHER".equalsIgnoreCase(sa.getRole()))
                            .findFirst()
                            .orElse(assignments.isEmpty() ? null : assignments.get(0));

                    Long teacherId = mainAssignment != null ? mainAssignment.getTeacher().getId() : null;
                    String teacherName = mainAssignment != null ? mainAssignment.getTeacher().getFullName() : null;
                    String teacherCode = mainAssignment != null ? mainAssignment.getTeacher().getStaffCode() : null;
                    String role = mainAssignment != null ? mainAssignment.getRole() : null;
                    boolean isSubstituted = false;

                    List<TeachingSubstitution> substitutions = teachingSubstitutionRepository.findByScheduleId(schedule.getId());
                    LocalDate finalDate = date;
                    TeachingSubstitution substitution = substitutions.stream()
                            .filter(ts -> "APPROVED".equalsIgnoreCase(ts.getStatus()) &&
                                    !finalDate.isBefore(ts.getStartDate()) && !finalDate.isAfter(ts.getEndDate()))
                            .findFirst()
                            .orElse(null);

                    if (substitution != null) {
                        teacherId = substitution.getSubstituteStaff().getId();
                        teacherName = substitution.getSubstituteStaff().getFullName();
                        teacherCode = substitution.getSubstituteStaff().getStaffCode();
                        role = "SUBSTITUTE_TEACHER";
                        isSubstituted = true;
                    }

                    timetable.add(TimetableEntryDto.builder()
                            .scheduleId(schedule.getId())
                            .classId(schedule.getClasses().getId())
                            .classCode(schedule.getClasses().getCode())
                            .className(schedule.getClasses().getName())
                            .roomName(schedule.getRoom() != null ? schedule.getRoom().getName() : "LMS/Online")
                            .date(date)
                            .startTime(schedule.getStartTime())
                            .endTime(schedule.getEndTime())
                            .teacherId(teacherId)
                            .teacherName(teacherName)
                            .teacherCode(teacherCode)
                            .role(role)
                            .isSubstituted(isSubstituted)
                            .build());
                }
            }
        }

        timetable.sort(Comparator.comparing(TimetableEntryDto::getDate)
                .thenComparing(TimetableEntryDto::getStartTime));

        return timetable;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getTeacherTimetable(Long teacherId, LocalDate startDate, LocalDate endDate) {
        if (!staffRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + teacherId);
        }

        List<ScheduleAssignment> assignments = scheduleAssignmentRepository.findByTeacherId(teacherId);
        List<TimetableEntryDto> timetable = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int systemDayOfWeek = date.getDayOfWeek().getValue() + 1;

            for (ScheduleAssignment assignment : assignments) {
                ClassSchedule schedule = assignment.getSchedule();
                if (schedule.getDayOfWeek() == systemDayOfWeek) {
                    List<TeachingSubstitution> substitutions = teachingSubstitutionRepository.findByScheduleId(schedule.getId());
                    LocalDate finalDate = date;
                    boolean isAbsent = substitutions.stream()
                            .anyMatch(ts -> "APPROVED".equalsIgnoreCase(ts.getStatus()) &&
                                    ts.getAbsentStaff().getId().equals(teacherId) &&
                                    !finalDate.isBefore(ts.getStartDate()) && !finalDate.isAfter(ts.getEndDate()));

                    if (!isAbsent) {
                        timetable.add(TimetableEntryDto.builder()
                                .scheduleId(schedule.getId())
                                .classId(schedule.getClasses().getId())
                                .classCode(schedule.getClasses().getCode())
                                .className(schedule.getClasses().getName())
                                .roomName(schedule.getRoom() != null ? schedule.getRoom().getName() : "LMS/Online")
                                .date(date)
                                .startTime(schedule.getStartTime())
                                .endTime(schedule.getEndTime())
                                .teacherId(teacherId)
                                .teacherName(assignment.getTeacher().getFullName())
                                .teacherCode(assignment.getTeacher().getStaffCode())
                                .role(assignment.getRole())
                                .isSubstituted(false)
                                .build());
                    }
                }
            }
        }

        List<TeachingSubstitution> subRequests = teachingSubstitutionRepository.findBySubstituteStaffId(teacherId);
        for (TeachingSubstitution sub : subRequests) {
            if ("APPROVED".equalsIgnoreCase(sub.getStatus())) {
                ClassSchedule schedule = sub.getSchedule();
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                    int systemDayOfWeek = date.getDayOfWeek().getValue() + 1;
                    if (schedule.getDayOfWeek() == systemDayOfWeek &&
                            !date.isBefore(sub.getStartDate()) && !date.isAfter(sub.getEndDate())) {

                        timetable.add(TimetableEntryDto.builder()
                                .scheduleId(schedule.getId())
                                .classId(schedule.getClasses().getId())
                                .classCode(schedule.getClasses().getCode())
                                .className(schedule.getClasses().getName())
                                .roomName(schedule.getRoom() != null ? schedule.getRoom().getName() : "LMS/Online")
                                .date(date)
                                .startTime(schedule.getStartTime())
                                .endTime(schedule.getEndTime())
                                .teacherId(teacherId)
                                .teacherName(sub.getSubstituteStaff().getFullName())
                                .teacherCode(sub.getSubstituteStaff().getStaffCode())
                                .role("SUBSTITUTE_TEACHER")
                                .isSubstituted(true)
                                .build());
                    }
                }
            }
        }

        timetable.sort(Comparator.comparing(TimetableEntryDto::getDate)
                .thenComparing(TimetableEntryDto::getStartTime));

        return timetable;
    }
}
