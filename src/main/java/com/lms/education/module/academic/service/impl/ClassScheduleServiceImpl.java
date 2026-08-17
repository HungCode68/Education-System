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
import com.lms.education.module.academic.repository.ScheduleCancellationRepository;
import com.lms.education.module.academic.service.ClassScheduleService;
import com.lms.education.module.academic.service.ClassesService;
import com.lms.education.module.academic.entity.ScheduleCancellation;
import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ClassesService classesService;
    private final RoomRepository roomRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingSubstitutionRepository teachingSubstitutionRepository;
    private final ScheduleCancellationRepository scheduleCancellationRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

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

        validateConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), classes.getStartDate(), classes.getEndDate(), null);

        ClassSchedule classSchedule = ClassSchedule.builder()
                .classes(classes)
                .room(room)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .build();

        ClassSchedule saved = classScheduleRepository.save(classSchedule);
        
        // Recalculate class end date
        classesService.recalculateEndDate(classes.getId());
        
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

        validateConflicts(dto.getClassId(), dto.getRoomId(), dto.getDayOfWeek(), dto.getStartTime(), dto.getEndTime(), classes.getStartDate(), classes.getEndDate(), id);

        classSchedule.setClasses(classes);
        classSchedule.setRoom(room);
        classSchedule.setDayOfWeek(dto.getDayOfWeek());
        classSchedule.setStartTime(dto.getStartTime());
        classSchedule.setEndTime(dto.getEndTime());

        ClassSchedule updated = classScheduleRepository.save(classSchedule);
        
        // Recalculate class end date
        classesService.recalculateEndDate(classes.getId());
        
        log.info("Đã cập nhật lịch học ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + id));
                
        Long classId = classSchedule.getClasses().getId();

        classScheduleRepository.delete(classSchedule);
        
        // Recalculate class end date
        classesService.recalculateEndDate(classId);
        
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

    private void validateConflicts(Long classId, Long roomId, Integer dayOfWeek, java.time.LocalTime start, java.time.LocalTime end, LocalDate startDate, LocalDate endDate, Long excludeId) {
        boolean isClassConflict = classScheduleRepository.existsClassConflict(classId, dayOfWeek, start, end, startDate, endDate, excludeId);
        if (isClassConflict) {
            throw new OperationNotPermittedException("Lớp học này đã có lịch học trùng với khoảng thời gian được chọn!");
        }

        if (roomId != null) {
            boolean isRoomConflict = classScheduleRepository.existsRoomConflict(roomId, dayOfWeek, start, end, startDate, endDate, excludeId);
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

                    boolean isCancelled = false;
                    String cancelReason = null;
                    List<ScheduleCancellation> cancellations = scheduleCancellationRepository.findByClassIdOrCenterWide(schedule.getClasses().getId());
                    for (ScheduleCancellation c : cancellations) {
                        if (!finalDate.isBefore(c.getStartDate()) && !finalDate.isAfter(c.getEndDate())) {
                            isCancelled = true;
                            cancelReason = c.getReason();
                            break;
                        }
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
                            .assignmentId(mainAssignment != null ? mainAssignment.getId() : null)
                            .isSubstituted(isSubstituted)
                            .status(isCancelled ? "CANCELLED" : "NORMAL")
                            .cancellationReason(cancelReason)
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

                    boolean isCancelled = false;
                    String cancelReason = null;
                    List<ScheduleCancellation> cancellations = scheduleCancellationRepository.findByClassIdOrCenterWide(schedule.getClasses().getId());
                    for (ScheduleCancellation c : cancellations) {
                        if (!finalDate.isBefore(c.getStartDate()) && !finalDate.isAfter(c.getEndDate())) {
                            isCancelled = true;
                            cancelReason = c.getReason();
                            break;
                        }
                    }

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
                                .status(isCancelled ? "CANCELLED" : "NORMAL")
                                .cancellationReason(cancelReason)
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

                        boolean isCancelled = false;
                        String cancelReason = null;
                        List<ScheduleCancellation> cancellations = scheduleCancellationRepository.findByClassIdOrCenterWide(schedule.getClasses().getId());
                        LocalDate finalDate = date;
                        for (ScheduleCancellation c : cancellations) {
                            if (!finalDate.isBefore(c.getStartDate()) && !finalDate.isAfter(c.getEndDate())) {
                                isCancelled = true;
                                cancelReason = c.getReason();
                                break;
                            }
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
                                .teacherName(sub.getSubstituteStaff().getFullName())
                                .teacherCode(sub.getSubstituteStaff().getStaffCode())
                                .role("SUBSTITUTE_TEACHER")
                                .isSubstituted(true)
                                .status(isCancelled ? "CANCELLED" : "NORMAL")
                                .cancellationReason(cancelReason)
                                .build());
                    }
                }
            }
        }

        timetable.sort(Comparator.comparing(TimetableEntryDto::getDate)
                .thenComparing(TimetableEntryDto::getStartTime));

        return timetable;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getMyTeacherTimetable(LocalDate startDate, LocalDate endDate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return List.of();
        }

        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return List.of();
        }

        Long userId = user.getId();
        Long staffId = staffRepository.findByUserId(userId).map(Staff::getId).orElse(null);
        if (staffId == null) {
            return List.of();
        }

        return getTeacherTimetable(staffId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimetableEntryDto> getTimetable(LocalDate startDate, LocalDate endDate, Long classId) {
        List<ClassSchedule> schedules;
        if (classId != null) {
            schedules = classScheduleRepository.findByClassesId(classId);
        } else {
            schedules = classScheduleRepository.findAll();
        }
        
        List<TimetableEntryDto> timetable = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int systemDayOfWeek = date.getDayOfWeek().getValue() + 1;

            for (ClassSchedule schedule : schedules) {
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
                    Long assignmentId = mainAssignment != null ? mainAssignment.getId() : null;
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

                    boolean isCancelled = false;
                    String cancelReason = null;
                    List<ScheduleCancellation> cancellations = scheduleCancellationRepository.findByClassIdOrCenterWide(schedule.getClasses().getId());
                    for (ScheduleCancellation c : cancellations) {
                        if (!finalDate.isBefore(c.getStartDate()) && !finalDate.isAfter(c.getEndDate())) {
                            isCancelled = true;
                            cancelReason = c.getReason();
                            break;
                        }
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
                            .assignmentId(assignmentId)
                            .isSubstituted(isSubstituted)
                            .status(isCancelled ? "CANCELLED" : "NORMAL")
                            .cancellationReason(cancelReason)
                            .build());
                }
            }
        }

        return timetable;
    }
}
