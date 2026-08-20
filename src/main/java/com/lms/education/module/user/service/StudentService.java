package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.StudentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StudentService {

    StudentDto create(StudentDto dto);

    StudentDto update(Long id, StudentDto dto);

    void delete(Long id);

    StudentDto getById(Long id);

    StudentDto getByStudentCode(String studentCode);

    Page<StudentDto> getAllStudents(String keyword, Pageable pageable);

    // Cấp tài khoản tự động hàng loạt cho học viên
    java.util.Map<String, Object> provisionAccounts(com.lms.education.module.user.dto.StudentProvisionDto dto);

    StudentDto getMyProfile(Long userId);
}