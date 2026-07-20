package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.teaching.dto.ScheduleAssignmentDto;
import com.lms.education.module.teaching.entity.ScheduleAssignment;
import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
import com.lms.education.module.teaching.service.ScheduleAssignmentService;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleAssignmentServiceImpl implements ScheduleAssignmentService {

    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public ScheduleAssignmentDto create(ScheduleAssignmentDto dto) {
        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + dto.getScheduleId()));

        Staff staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + dto.getStaffId()));

        if (!staff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên được chọn không phải là Giáo viên!");
        }

        // Kiểm tra xem Giáo viên đã được phân công tổng thể cho Lớp học này trong teaching_assignments hay chưa
        boolean isAssignedToClass = teachingAssignmentRepository.existsByTeacherIdAndClassesId(dto.getStaffId(), schedule.getClasses().getId());
        if (!isAssignedToClass) {
            throw new OperationNotPermittedException("Giáo viên '" + staff.getFullName() + "' chưa được Bộ phận Đào tạo phân công chính thức cho lớp học '" + schedule.getClasses().getName() + "'!");
        }

        if (scheduleAssignmentRepository.existsByScheduleIdAndTeacherId(dto.getScheduleId(), dto.getStaffId())) {
            throw new DuplicateResourceException("Giáo viên này đã được phân công cho ca học này rồi!");
        }

        // Kiểm tra xem Giáo viên có bị trùng lịch dạy ở ca học khác cùng khung giờ hay không
        boolean hasTeacherConflict = scheduleAssignmentRepository.existsTeacherConflict(
                dto.getStaffId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                null
        );
        if (hasTeacherConflict) {
            throw new DuplicateResourceException("Giáo viên '" + staff.getFullName() + "' đã có ca dạy khác trùng với khung giờ này (Thứ " + schedule.getDayOfWeek() + ", " + schedule.getStartTime() + " - " + schedule.getEndTime() + ")!");
        }

        ScheduleAssignment assignment = ScheduleAssignment.builder()
                .schedule(schedule)
                .teacher(staff)
                .role(dto.getRole() != null && !dto.getRole().trim().isEmpty() ? dto.getRole().trim().toUpperCase() : "MAIN_TEACHER")
                .build();

        ScheduleAssignment saved = scheduleAssignmentRepository.save(assignment);
        log.info("Đã phân công giáo viên: {} cho ca học ID: {} của lớp: {}",
                staff.getFullName(), schedule.getId(), schedule.getClasses().getName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public ScheduleAssignmentDto update(Long id, ScheduleAssignmentDto dto) {
        ScheduleAssignment existing = scheduleAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công ca học với ID: " + id));

        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + dto.getScheduleId()));

        Staff staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + dto.getStaffId()));

        if (!staff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên được chọn không phải là Giáo viên!");
        }

        // Kiểm tra xem Giáo viên đã được phân công tổng thể cho Lớp học này trong teaching_assignments hay chưa
        boolean isAssignedToClass = teachingAssignmentRepository.existsByTeacherIdAndClassesId(dto.getStaffId(), schedule.getClasses().getId());
        if (!isAssignedToClass) {
            throw new OperationNotPermittedException("Giáo viên '" + staff.getFullName() + "' chưa được Bộ phận Đào tạo phân công chính thức cho lớp học '" + schedule.getClasses().getName() + "'!");
        }

        if ((!existing.getSchedule().getId().equals(dto.getScheduleId()) || !existing.getTeacher().getId().equals(dto.getStaffId()))
                && scheduleAssignmentRepository.existsByScheduleIdAndTeacherId(dto.getScheduleId(), dto.getStaffId())) {
            throw new DuplicateResourceException("Giáo viên này đã được phân công cho ca học này rồi!");
        }

        // Kiểm tra xem Giáo viên có bị trùng lịch dạy ở ca học khác cùng khung giờ hay không
        boolean hasTeacherConflict = scheduleAssignmentRepository.existsTeacherConflict(
                dto.getStaffId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                id
        );
        if (hasTeacherConflict) {
            throw new DuplicateResourceException("Giáo viên '" + staff.getFullName() + "' đã có ca dạy khác trùng với khung giờ này (Thứ " + schedule.getDayOfWeek() + ", " + schedule.getStartTime() + " - " + schedule.getEndTime() + ")!");
        }

        existing.setSchedule(schedule);
        existing.setTeacher(staff);
        if (dto.getRole() != null && !dto.getRole().trim().isEmpty()) {
            existing.setRole(dto.getRole().trim().toUpperCase());
        }

        ScheduleAssignment updated = scheduleAssignmentRepository.save(existing);
        log.info("Đã cập nhật phân công ca học ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ScheduleAssignment existing = scheduleAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công ca học với ID: " + id));

        scheduleAssignmentRepository.delete(existing);
        log.info("Đã xóa phân công ca học ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ScheduleAssignmentDto getById(Long id) {
        ScheduleAssignment existing = scheduleAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công ca học với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScheduleAssignmentDto> getAll(String keyword, Pageable pageable) {
        Page<ScheduleAssignment> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = scheduleAssignmentRepository.searchAssignments(keyword.trim(), pageable);
        } else {
            page = scheduleAssignmentRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleAssignmentDto> getAssignmentsByClassId(Long classId) {
        return scheduleAssignmentRepository.findByScheduleClassesId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleAssignmentDto> getAssignmentsByScheduleId(Long scheduleId) {
        return scheduleAssignmentRepository.findByScheduleId(scheduleId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ScheduleAssignmentDto mapToDto(ScheduleAssignment entity) {
        return ScheduleAssignmentDto.builder()
                .id(entity.getId())
                .scheduleId(entity.getSchedule().getId())
                .classCode(entity.getSchedule().getClasses().getCode())
                .className(entity.getSchedule().getClasses().getName())
                .staffId(entity.getTeacher().getId())
                .staffCode(entity.getTeacher().getStaffCode())
                .teacherName(entity.getTeacher().getFullName())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
