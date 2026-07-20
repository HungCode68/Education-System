package com.lms.education.module.teaching.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.OperationNotPermittedException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.Classes;
import com.lms.education.module.academic.repository.ClassesRepository;
import com.lms.education.module.teaching.dto.TeachingAssignmentDto;
import com.lms.education.module.teaching.entity.TeachingAssignment;
import com.lms.education.module.teaching.repository.TeachingAssignmentRepository;
import com.lms.education.module.teaching.service.TeachingAssignmentService;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.repository.StaffRepository;
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
public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {

    private final TeachingAssignmentRepository teachingAssignmentRepository;
    private final StaffRepository staffRepository;
    private final ClassesRepository classesRepository;

    @Override
    @Transactional
    public TeachingAssignmentDto create(TeachingAssignmentDto dto) {
        Staff staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + dto.getStaffId()));

        if (!staff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên được chọn không phải là Giáo viên!");
        }

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        if (teachingAssignmentRepository.existsByTeacherIdAndClassesId(dto.getStaffId(), dto.getClassId())) {
            throw new DuplicateResourceException("Giáo viên này đã được phân công cho lớp học này rồi!");
        }

        java.time.LocalDate assignedDate = dto.getAssignedDate() != null ? dto.getAssignedDate() : java.time.LocalDate.now();
        if (dto.getEndDate() != null && assignedDate.isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày phân công phải trước hoặc bằng ngày kết thúc!");
        }

        TeachingAssignment assignment = TeachingAssignment.builder()
                .teacher(staff)
                .classes(classes)
                .role(dto.getRole() != null ? dto.getRole().trim().toUpperCase() : "MAIN_TEACHER")
                .assignedDate(assignedDate)
                .endDate(dto.getEndDate())
                .status(dto.getStatus() != null ? dto.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();

        TeachingAssignment saved = teachingAssignmentRepository.save(assignment);
        log.info("Đã phân công giáo viên: {} cho lớp: {} (Mã: {})",
                staff.getFullName(), classes.getName(), classes.getCode());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TeachingAssignmentDto update(Long id, TeachingAssignmentDto dto) {
        TeachingAssignment existing = teachingAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công giảng dạy với ID: " + id));

        Staff staff = staffRepository.findById(dto.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên với ID: " + dto.getStaffId()));

        if (!staff.getStaffType().equalsIgnoreCase("TEACHER")) {
            throw new OperationNotPermittedException("Nhân viên được chọn không phải là Giáo viên!");
        }

        Classes classes = classesRepository.findById(dto.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + dto.getClassId()));

        if ((!existing.getTeacher().getId().equals(dto.getStaffId()) || !existing.getClasses().getId().equals(dto.getClassId()))
                && teachingAssignmentRepository.existsByTeacherIdAndClassesId(dto.getStaffId(), dto.getClassId())) {
            throw new DuplicateResourceException("Giáo viên này đã được phân công cho lớp học này rồi!");
        }

        java.time.LocalDate assignedDate = dto.getAssignedDate() != null ? dto.getAssignedDate() : existing.getAssignedDate();
        if (dto.getEndDate() != null && assignedDate.isAfter(dto.getEndDate())) {
            throw new OperationNotPermittedException("Ngày phân công phải trước hoặc bằng ngày kết thúc!");
        }

        existing.setTeacher(staff);
        existing.setClasses(classes);
        existing.setAssignedDate(assignedDate);
        existing.setEndDate(dto.getEndDate());
        if (dto.getRole() != null && !dto.getRole().trim().isEmpty()) {
            existing.setRole(dto.getRole().trim().toUpperCase());
        }
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            existing.setStatus(dto.getStatus().trim().toUpperCase());
        }

        TeachingAssignment updated = teachingAssignmentRepository.save(existing);
        log.info("Đã cập nhật phân công giảng dạy ID: {}", id);

        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        TeachingAssignment existing = teachingAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công giảng dạy với ID: " + id));

        teachingAssignmentRepository.delete(existing);
        log.info("Đã xóa phân công giảng dạy ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TeachingAssignmentDto getById(Long id) {
        TeachingAssignment existing = teachingAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản phân công giảng dạy với ID: " + id));
        return mapToDto(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TeachingAssignmentDto> getAllAssignments(String keyword, Pageable pageable) {
        Page<TeachingAssignment> page;
        if (keyword != null && !keyword.trim().isEmpty()) {
            page = teachingAssignmentRepository.searchAssignments(keyword.trim(), pageable);
        } else {
            page = teachingAssignmentRepository.findAll(pageable);
        }
        return page.map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeachingAssignmentDto> getAssignmentsByClassId(Long classId) {
        if (!classesRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Không tìm thấy lớp học với ID: " + classId);
        }
        return teachingAssignmentRepository.findByClassesId(classId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private TeachingAssignmentDto mapToDto(TeachingAssignment entity) {
        return TeachingAssignmentDto.builder()
                .id(entity.getId())
                .staffId(entity.getTeacher().getId())
                .staffCode(entity.getTeacher().getStaffCode())
                .teacherName(entity.getTeacher().getFullName())
                .classId(entity.getClasses().getId())
                .classCode(entity.getClasses().getCode())
                .className(entity.getClasses().getName())
                .role(entity.getRole())
                .assignedDate(entity.getAssignedDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
