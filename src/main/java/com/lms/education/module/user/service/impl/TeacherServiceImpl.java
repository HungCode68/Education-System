package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.TeacherDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.module.user.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public TeacherDto create(TeacherDto dto) {
        if (teacherRepository.existsByTeacherCode(dto.getTeacherCode())) {
            throw new DuplicateResourceException("Teacher code already exists: " + dto.getTeacherCode());
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
        }

        Teacher teacher = Teacher.builder()
                .teacherCode(dto.getTeacherCode())
                .fullName(dto.getFullName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .phone(dto.getPhone())
                .emailContact(dto.getEmailContact())
                .address(dto.getAddress())
                .department(department)
                .position(dto.getPosition())
                .degree(dto.getDegree())
                .major(dto.getMajor())
                .startDate(dto.getStartDate())
                .status(dto.getStatus() != null ? dto.getStatus() : Teacher.Status.working)
                .build();

        Teacher saved = teacherRepository.save(teacher);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TeacherDto update(String id, TeacherDto dto) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));

        if (!teacher.getTeacherCode().equals(dto.getTeacherCode()) &&
                teacherRepository.existsByTeacherCode(dto.getTeacherCode())) {
            throw new DuplicateResourceException("Teacher code already exists: " + dto.getTeacherCode());
        }

        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + dto.getDepartmentId()));
            teacher.setDepartment(department);
        } else {
            teacher.setDepartment(null);
        }

        teacher.setTeacherCode(dto.getTeacherCode());
        teacher.setFullName(dto.getFullName());
        teacher.setDateOfBirth(dto.getDateOfBirth());
        teacher.setGender(dto.getGender());
        teacher.setPhone(dto.getPhone());
        teacher.setEmailContact(dto.getEmailContact());
        teacher.setAddress(dto.getAddress());
        teacher.setPosition(dto.getPosition());
        teacher.setDegree(dto.getDegree());
        teacher.setMajor(dto.getMajor());
        teacher.setStartDate(dto.getStartDate());
        if (dto.getStatus() != null) {
            teacher.setStatus(dto.getStatus());
        }

        Teacher updated = teacherRepository.save(teacher);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!teacherRepository.existsById(id)) {
            throw new ResourceNotFoundException("Teacher not found with id: " + id);
        }
        teacherRepository.deleteById(id);
    }

    @Override
    public TeacherDto getById(String id) {
        Teacher teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + id));
        return mapToDto(teacher);
    }

    @Override
    public Page<TeacherDto> getAll(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::mapToDto);
    }

    private TeacherDto mapToDto(Teacher teacher) {
        return TeacherDto.builder()
                .id(teacher.getId())
                .teacherCode(teacher.getTeacherCode())
                .fullName(teacher.getFullName())
                .dateOfBirth(teacher.getDateOfBirth())
                .gender(teacher.getGender())
                .phone(teacher.getPhone())
                .emailContact(teacher.getEmailContact())
                .address(teacher.getAddress())
                .departmentId(teacher.getDepartment() != null ? teacher.getDepartment().getId() : null)
                .position(teacher.getPosition())
                .degree(teacher.getDegree())
                .major(teacher.getMajor())
                .startDate(teacher.getStartDate())
                .status(teacher.getStatus())
                .build();
    }
}
