package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {
    StudentDto create(StudentDto dto);
    StudentDto update(String id, StudentDto dto);
    void delete(String id);
    StudentDto getById(String id);
    Page<StudentDto> getAll(String keyword, Student.Status status, Integer admissionYear, Pageable pageable);
    void createAccountForExistingStudent(String studentId, String email);
    java.util.Map<String, Object> createAccountsBatch(java.util.List<String> studentIds);
}
