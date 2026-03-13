package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.academic.entity.PhysicalClass;
import com.lms.education.module.academic.repository.PhysicalClassRepository;
import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public StudentDto create(StudentDto dto) {
        if (studentRepository.existsByStudentCode(dto.getStudentCode())) {
            throw new DuplicateResourceException("Mã học sinh đã tồn tại: " + dto.getStudentCode());
        }

        PhysicalClass physicalClass = null;
        if (dto.getCurrentClassId() != null) {
            physicalClass = physicalClassRepository.findById(dto.getCurrentClassId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lớp không tồn tại"));
        }

        User savedUser = null;

        // Chỉ tạo tài khoản nếu có nhập Email
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new DuplicateResourceException("Email đã tồn tại: " + dto.getEmail());
            }
            User newUser = new User();
            newUser.setEmail(dto.getEmail());
            newUser.setPassword(passwordEncoder.encode(dto.getStudentCode()));
            Role studentRole = roleRepository.findByCode("STUDENT")
                    .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: Chưa có role ROLE_STUDENT trong Database"));

            newUser.setRole(studentRole);
            savedUser = userRepository.save(newUser);
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
                .user(savedUser)
                .build();

        return mapToDto(studentRepository.save(student));
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

    @Override
    @Transactional
    public void createAccountForExistingStudent(String studentId, String email) {
        // Tìm hồ sơ học sinh
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Học sinh không tồn tại"));

        if (student.getUser() != null) {
            throw new DuplicateResourceException("Học sinh này đã có tài khoản rồi!");
        }

        String finalEmail;

        if (email != null && !email.isBlank()) {
            finalEmail = email;
        } else {
            // TỰ ĐỘNG: Lấy Mã SV viết thường + đuôi email
            // Ví dụ: StudentCode="HS2024001" -> Email="hs2024001@school.edu.vn"
            finalEmail = student.getStudentCode().toLowerCase() + "@school.edu.vn";
        }

        // Check trùng lần cuối
        if (userRepository.existsByEmail(finalEmail)) {
            throw new DuplicateResourceException("Email " + finalEmail + " đã tồn tại trong hệ thống!");
        }

        // Tạo User
        User newUser = new User();
        newUser.setEmail(finalEmail);
        // Mật khẩu mặc định vẫn để là Mã SV (viết hoa đúng như hồ sơ) cho dễ nhớ
        newUser.setPassword(passwordEncoder.encode(student.getStudentCode()));

        // Lấy Role
        com.lms.education.module.user.entity.Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new RuntimeException("Lỗi: DB thiếu role STUDENT"));
        newUser.setRole(studentRole);

        User savedUser = userRepository.save(newUser);

        // Update ngược lại vào Student
        student.setUser(savedUser);
        studentRepository.save(student);
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
                .email(student.getUser() != null ? student.getUser().getEmail() : null)
                .userId(student.getUser() != null ? student.getUser().getId() : null)
                .build();
    }
}
