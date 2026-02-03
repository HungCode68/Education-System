package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final PhysicalClassRepository physicalClassRepository;

    @Override
    @Transactional
    public StudentDto create(StudentDto dto) {
        if (studentRepository.existsByStudentCode(dto.getStudentCode())) {
            throw new DuplicateResourceException("Student code already exists: " + dto.getStudentCode());
        }

        PhysicalClass physicalClass = null;
        if (dto.getCurrentClassId() != null) {
            physicalClass = physicalClassRepository.findById(dto.getCurrentClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + dto.getCurrentClassId()));
        }

        Student student = Student.builder()
                .studentCode(dto.getStudentCode())
                .fullName(dto.getFullName())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .currentClass(physicalClass)
                .address(dto.getAddress())
                .parentPhone(dto.getParentPhone())
                .parentName(dto.getParentName())
                .admissionYear(dto.getAdmissionYear())
                .status(dto.getStatus() != null ? dto.getStatus() : Student.Status.studying)
                .build();

        Student savedStudent = studentRepository.save(student);
        return mapToDto(savedStudent);
    }

    @Override
    @Transactional
    public StudentDto update(String id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));

        if (!student.getStudentCode().equals(dto.getStudentCode()) && studentRepository.existsByStudentCode(dto.getStudentCode())) {
            throw new DuplicateResourceException("Student code already exists: " + dto.getStudentCode());
        }

        if (dto.getCurrentClassId() != null) {
            PhysicalClass physicalClass = physicalClassRepository.findById(dto.getCurrentClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + dto.getCurrentClassId()));
            student.setCurrentClass(physicalClass);
        } else {
            student.setCurrentClass(null);
        }

        student.setStudentCode(dto.getStudentCode());
        student.setFullName(dto.getFullName());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setParentPhone(dto.getParentPhone());
        student.setParentName(dto.getParentName());
        student.setAdmissionYear(dto.getAdmissionYear());
        if (dto.getStatus() != null) {
            student.setStatus(dto.getStatus());
        }

        Student updatedStudent = studentRepository.save(student);
        return mapToDto(updatedStudent);
    }

    @Override
    @Transactional
    public void delete(String id) {
        if (!studentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Override
    public StudentDto getById(String id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + id));
        return mapToDto(student);
    }

    @Override
    public Page<StudentDto> getAll(Pageable pageable) {
        return studentRepository.findAll(pageable).map(this::mapToDto);
    }

    private StudentDto mapToDto(Student student) {
        return StudentDto.builder()
                .id(student.getId())
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .currentClassId(student.getCurrentClass() != null ? student.getCurrentClass().getId() : null)
                .address(student.getAddress())
                .parentPhone(student.getParentPhone())
                .parentName(student.getParentName())
                .admissionYear(student.getAdmissionYear())
                .status(student.getStatus())
                .build();
    }
}
