package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.ClassSchedule;
import com.lms.education.module.academic.repository.ClassScheduleRepository;
import com.lms.education.module.teaching.dto.TeachingSubstitutionDto;
import com.lms.education.module.teaching.entity.TeachingSubstitution;
import com.lms.education.module.teaching.repository.TeachingSubstitutionRepository;
import com.lms.education.module.teaching.service.TeachingSubstitutionService;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lms.education.module.teaching.repository.ScheduleAssignmentRepository;
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
public class TeachingSubstitutionServiceImpl implements TeachingSubstitutionService {

    private final TeachingSubstitutionRepository teachingSubstitutionRepository;
    private final ScheduleAssignmentRepository scheduleAssignmentRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final StaffRepository staffRepository;

    @Override
    @Transactional
    public TeachingSubstitutionDto create(TeachingSubstitutionDto dto) {
        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + dto.getScheduleId()));

        Staff absentStaff = staffRepository.findById(dto.getAbsentStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên vắng mặt với ID: " + dto.getAbsentStaffId()));

        if (!absentStaff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên vắng mặt được chọn không phải là Giáo viên!");
        }

        Staff substituteStaff = staffRepository.findById(dto.getSubstituteStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên dạy thay với ID: " + dto.getSubstituteStaffId()));

        if (!substituteStaff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên dạy thay được chọn không phải là Giáo viên!");
        }

        if (absentStaff.getId().equals(substituteStaff.getId())) {
            throw new OperationNotPermittedException("Giáo viên dạy thay không được trùng với giáo viên vắng mặt!");
        }

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu dạy thay phải trước hoặc bằng ngày kết thúc!");
        }

        // Kiểm tra xem Giáo viên dạy thay có bị trùng lịch dạy chính ở lớp khác trong khung giờ này hay không
        boolean hasRegularScheduleConflict = scheduleAssignmentRepository.existsTeacherConflict(
                substituteStaff.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                null
        );
        if (hasRegularScheduleConflict) {
            throw new DuplicateResourceException("Giáo viên dạy thay '" + substituteStaff.getFullName() + "' đã có ca dạy chính trùng với khung giờ này (Thứ " + schedule.getDayOfWeek() + ", " + schedule.getStartTime() + " - " + schedule.getEndTime() + ")!");
        }

        // Kiểm tra xem Giáo viên dạy thay có bị trùng lịch dạy thay khác đã duyệt hay không
        boolean hasSubConflict = teachingSubstitutionRepository.existsSubstituteConflict(
                substituteStaff.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                dto.getStartDate(),
                dto.getEndDate(),
                null
        );
        if (hasSubConflict) {
            throw new DuplicateResourceException("Giáo viên dạy thay '" + substituteStaff.getFullName() + "' đã có lịch dạy thay khác trùng với khung giờ và khoảng thời gian này!");
        }

        TeachingSubstitution substitution = TeachingSubstitution.builder()
                .schedule(schedule)
                .absentStaff(absentStaff)
                .substituteStaff(substituteStaff)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason().trim())
                .status(dto.getStatus() != null ? dto.getStatus().trim().toUpperCase() : "APPROVED")
                .build();

        TeachingSubstitution saved = teachingSubstitutionRepository.save(substitution);
        log.info("Đã tạo phân công dạy thay: Lớp {} (mã {}), từ giáo viên {} sang {}",
                schedule.getClasses().getName(), schedule.getClasses().getCode(),
                absentStaff.getFullName(), substituteStaff.getFullName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TeachingSubstitutionDto update(Long id, TeachingSubstitutionDto dto) {
        TeachingSubstitution existing = teachingSubstitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu dạy thay với ID: " + id));

        ClassSchedule schedule = classScheduleRepository.findById(dto.getScheduleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + dto.getScheduleId()));

        Staff absentStaff = staffRepository.findById(dto.getAbsentStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên vắng mặt với ID: " + dto.getAbsentStaffId()));

        if (!absentStaff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên vắng mặt được chọn không phải là Giáo viên!");
        }

        Staff substituteStaff = staffRepository.findById(dto.getSubstituteStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên dạy thay với ID: " + dto.getSubstituteStaffId()));

        if (!substituteStaff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên dạy thay được chọn không phải là Giáo viên!");
        }

        if (absentStaff.getId().equals(substituteStaff.getId())) {
            throw new OperationNotPermittedException("Giáo viên dạy thay không được trùng với giáo viên vắng mặt!");
        }

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày bắt đầu dạy thay phải trước hoặc bằng ngày kết thúc!");
        }

        // Kiểm tra xem Giáo viên dạy thay có bị trùng lịch dạy chính ở lớp khác trong khung giờ này hay không
        boolean hasRegularScheduleConflict = scheduleAssignmentRepository.existsTeacherConflict(
                substituteStaff.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                null
        );
        if (hasRegularScheduleConflict) {
            throw new DuplicateResourceException("Giáo viên dạy thay '" + substituteStaff.getFullName() + "' đã có ca dạy chính trùng với khung giờ này (Thứ " + schedule.getDayOfWeek() + ", " + schedule.getStartTime() + " - " + schedule.getEndTime() + ")!");
        }

        // Kiểm tra xem Giáo viên dạy thay có bị trùng lịch dạy thay khác đã duyệt hay không
        boolean hasSubConflict = teachingSubstitutionRepository.existsSubstituteConflict(
                substituteStaff.getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                dto.getStartDate(),
                dto.getEndDate(),
                id // exclude current id
        );
        if (hasSubConflict) {
            throw new DuplicateResourceException("Giáo viên dạy thay '" + substituteStaff.getFullName() + "' đã có lịch dạy thay khác trùng với khung giờ và khoảng thời gian này!");
        }

        existing.setSchedule(schedule);
        existing.setAbsentStaff(absentStaff);
        existing.setSubstituteStaff(substituteStaff);
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setReason(dto.getReason().trim());
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            existing.setStatus(dto.getStatus().trim().toUpperCase());
        }

        TeachingSubstitution updated = teachingSubstitutionRepository.save(existing);
        log.info("Đã cập nhật yêu cầu dạy thay ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeachingSubstitution existing = teachingSubstitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu dạy thay với ID: " + id));

        teachingSubstitutionRepository.delete(existing);
        log.info("Đã xóa yêu cầu dạy thay ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingSubstitutionDto getById(Long id) {
        TeachingSubstitution existing = teachingSubstitutionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu dạy thay với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingSubstitutionDto> getAll(String keyword, Pageable pageable) {
        Page<TeachingSubstitution> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = teachingSubstitutionRepository.searchSubstitutions(keyword.trim(), pageable);
        } else {
            page = teachingSubstitutionRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingSubstitutionDto> getSubstitutionsByClassId(Long classId) {
        return teachingSubstitutionRepository.findByScheduleClassesId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.lms.education.module.user.dto.StaffDto> getAvailableTeachers(
            Long scheduleId,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            Long excludeSubstitutionId) {

        ClassSchedule schedule = classScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học với ID: " + scheduleId));

        if (startDate.isAfter(endDate)) {
            throw new OperationNotPermittedException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc!");
        }

        List<Staff> teachers = staffRepository.findByStaffTypeContainingIgnoreCase("TEACHER");

        return teachers.stream()
                .filter(staff -> {
                    boolean hasRegularConflict = scheduleAssignmentRepository.existsTeacherConflict(
                            staff.getId(),
                            schedule.getDayOfWeek(),
                            schedule.getStartTime(),
                            schedule.getEndTime(),
                            null
                    );
                    if (hasRegularConflict) return false;

                    boolean hasSubConflict = teachingSubstitutionRepository.existsSubstituteConflict(
                            staff.getId(),
                            schedule.getDayOfWeek(),
                            schedule.getStartTime(),
                            schedule.getEndTime(),
                            startDate,
                            endDate,
                            excludeSubstitutionId
                    );
                    return !hasSubConflict;
                })
                .map(staff -> com.lms.education.module.user.dto.StaffDto.builder()
                        .id(staff.getId())
                        .userId(staff.getUser() != null ? staff.getUser().getId() : null)
                        .userEmail(staff.getUser() != null ? staff.getUser().getEmail() : null)
                        .departmentId(staff.getDepartment() != null ? staff.getDepartment().getId() : null)
                        .departmentName(staff.getDepartment() != null ? staff.getDepartment().getName() : null)
                        .staffCode(staff.getStaffCode())
                        .staffType(staff.getStaffType())
                        .fullName(staff.getFullName())
                        .phone(staff.getPhone())
                        .hireDate(staff.getHireDate())
                        .contractType(staff.getContractType())
                        .baseSalary(staff.getBaseSalary())
                        .status(staff.getStatus())
                        .createdAt(staff.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private TeachingSubstitutionDto mapToDto(TeachingSubstitution entity) {
        com.lms.education.module.academic.entity.ClassSchedule sch = entity.getSchedule();
        return TeachingSubstitutionDto.builder()
                .id(entity.getId())
                .scheduleId(sch.getId())
                .dayOfWeek(sch.getDayOfWeek())
                .startTime(sch.getStartTime() != null ? sch.getStartTime().toString() : null)
                .endTime(sch.getEndTime() != null ? sch.getEndTime().toString() : null)
                .roomName(sch.getRoom() != null ? sch.getRoom().getName() : null)
                .classCode(sch.getClasses().getCode())
                .className(sch.getClasses().getName())
                .absentStaffId(entity.getAbsentStaff().getId())
                .absentStaffName(entity.getAbsentStaff().getFullName())
                .absentStaffCode(entity.getAbsentStaff().getStaffCode())
                .substituteStaffId(entity.getSubstituteStaff().getId())
                .substituteStaffName(entity.getSubstituteStaff().getFullName())
                .substituteStaffCode(entity.getSubstituteStaff().getStaffCode())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
