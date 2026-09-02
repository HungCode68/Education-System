package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.StudentDto;
import com.lms.education.module.user.dto.StudentProvisionDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.service.StudentService;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentServiceImpl implements StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final com.lms.education.module.enrollment.repository.EnrollmentRepository enrollmentRepository;

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private StudentService self;

    @Override
    @Transactional
    public StudentDto create(StudentDto dto) {
        // Kiểm tra tính hợp lệ của User (Chỉ kiểm tra khi có truyền userId của tài khoản có sẵn)
        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản User với ID: " + dto.getUserId()));

            if (studentRepository.existsByUserId(user.getId())) {
                throw new DuplicateResourceException("Tài khoản này đã được liên kết với một hồ sơ học viên khác!");
            }
        }

        // Tự động sinh mã học viên (HV + 2 số cuối năm + 4 số thứ tự)
        String prefix = "HV";
        String currentYearLastTwoDigits = String.valueOf(java.time.LocalDate.now().getYear() % 100);

        long currentCount = studentRepository.countByStudentCodePattern(prefix, currentYearLastTwoDigits);
        long nextSequence = currentCount + 1;
        String formattedSequence = String.format("%04d", nextSequence);
        String generatedStudentCode = prefix + currentYearLastTwoDigits + formattedSequence; // KQ: HV260001

        // Xây dựng Object Student
        Student student = Student.builder()
                .user(user)
                .studentCode(generatedStudentCode)
                .fullName(dto.getFullName().trim())
                .parentName(dto.getParentName() != null ? dto.getParentName().trim() : null)
                .parentPhone(dto.getParentPhone())
                .dateOfBirth(dto.getDateOfBirth())
                .gender(dto.getGender())
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .identityNumber(dto.getIdentityNumber())
                .targetScore(dto.getTargetScore())
                .status(dto.getStatus() != null ? dto.getStatus().toUpperCase() : "STUDYING")
                .build();

        Student savedStudent = studentRepository.save(student);
        log.info("Đã tạo hồ sơ học viên (Chưa cấp tài khoản): {}", savedStudent.getStudentCode());

        return mapToDto(savedStudent);
    }

    @org.springframework.context.annotation.Lazy
    @org.springframework.beans.factory.annotation.Autowired
    private com.lms.education.module.enrollment.service.EnrollmentService enrollmentService;

    @Override
    @Transactional
    public StudentDto update(Long id, StudentDto dto) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học viên với ID: " + id));

        // Kiểm tra đổi User
        if (dto.getUserId() != null && (student.getUser() == null || !student.getUser().getId().equals(dto.getUserId()))) {
            if (studentRepository.existsByUserId(dto.getUserId())) {
                throw new DuplicateResourceException("Tài khoản User mới này đã thuộc về học viên khác!");
            }
            User newUser = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản User với ID: " + dto.getUserId()));
            student.setUser(newUser);
        }

        // Cập nhật thông tin cơ bản (Không cập nhật studentCode)
        student.setFullName(dto.getFullName().trim());
        student.setParentName(dto.getParentName() != null ? dto.getParentName().trim() : null);
        student.setParentPhone(dto.getParentPhone());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setGender(dto.getGender());
        student.setAddress(dto.getAddress());
        student.setPhone(dto.getPhone());
        student.setIdentityNumber(dto.getIdentityNumber());
        student.setTargetScore(dto.getTargetScore());

        String oldStatus = student.getStatus();
        
        // Cập nhật trạng thái
        if (dto.getStatus() != null && !dto.getStatus().trim().isEmpty()) {
            student.setStatus(dto.getStatus().toUpperCase());
        }

        Student updatedStudent = studentRepository.save(student);
        
        // Kích hoạt đồng bộ trạng thái lớp học (Cascade)
        String newStatus = updatedStudent.getStatus();
        if (newStatus != null && !newStatus.equalsIgnoreCase(oldStatus) && 
            ("DROPPED".equalsIgnoreCase(newStatus) || "RESERVED".equalsIgnoreCase(newStatus))) {
            enrollmentService.handleStudentStatusCascade(updatedStudent.getId(), newStatus);
        }
        
        log.info("Đã cập nhật thông tin học viên ID: {}, Trạng thái: {}", id, updatedStudent.getStatus());

        return mapToDto(updatedStudent);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học viên với ID: " + id));
        
        User user = student.getUser();
        // Ngắt liên kết để tránh lỗi JPA Cascade (Dù CSDL có ON DELETE CASCADE, nhưng báo trước cho Hibernate)
        student.setUser(null);
        studentRepository.delete(student);
        
        if (user != null) {
            userRepository.delete(user);
        }
        
        log.info("Đã xóa hoàn toàn hồ sơ học viên ID: {} và tài khoản User liên quan", id);
    }

    @Override
    public Map<String, Object> deleteMultiple(List<Long> ids) {
        int successCount = 0;
        int skipCount = 0;

        for (Long id : ids) {
            try {
                self.delete(id);
                successCount++;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Bỏ qua học viên ID {} vì vướng dữ liệu liên quan (Foreign Key)", id);
                skipCount++;
            } catch (Exception e) {
                log.error("Lỗi khi xóa học viên ID {}: {}", id, e.getMessage());
                skipCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("success", successCount);
        report.put("skipped", skipCount);
        report.put("totalProcessed", ids.size());
        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với ID: " + id));
        return mapToDto(student);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getByStudentCode(String studentCode) {
        String formattedCode = studentCode.trim().toUpperCase();
        Student student = studentRepository.findByStudentCode(formattedCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên với mã: " + formattedCode));
        return mapToDto(student);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentDto> getAllStudents(String keyword, Pageable pageable) {
        Page<Student> students;
        if (keyword != null && !keyword.trim().isEmpty()) {
            students = studentRepository.searchStudents(keyword.trim(), pageable);
        } else {
            students = studentRepository.findAll(pageable);
        }
        return students.map(this::mapToDto);
    }


    @Override
    @Transactional
    public Map<String, Object> provisionAccounts(StudentProvisionDto dto) {
        // Lấy danh sách Role cần gán
        List<Role> roles = roleRepository.findAllById(dto.getRoleIds());
        if (roles.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy vai trò hợp lệ nào để gán cho học viên!");
        }

        // Lấy danh sách học viên cần xử lý
        List<Student> students = studentRepository.findAllById(dto.getStudentIds());

        int successCount = 0;
        int skipCount = 0;

        for (Student student : students) {
            // Chỉ cấp tài khoản cho những học viên CHƯA CÓ tài khoản gắn vào hồ sơ
            if (student.getUser() == null) {
                String generatedEmail = student.getStudentCode().toLowerCase() + "@student.edu.vn";
                String rawPassword = student.getStudentCode().toUpperCase();

                // Kiểm tra tránh trùng lặp email hệ thống
                if (userRepository.existsByEmail(generatedEmail)) {
                    skipCount++;
                    continue;
                }

                // Tạo User mới
                User newUser = User.builder()
                        .email(generatedEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .fullName(student.getFullName())
                        .status("ACTIVE")
                        .roles(new java.util.HashSet<>(roles))
                        .build();

                User savedUser = userRepository.save(newUser);

                // Gắn tài khoản vừa tạo ngược lại vào hồ sơ học viên
                student.setUser(savedUser);
                successCount++;
                log.info("Đã cấp tài khoản {} cho học viên {}", generatedEmail, student.getStudentCode());
            } else {
                skipCount++;
            }
        }

        // Trả về thống kê tiến trình
        Map<String, Object> report = new HashMap<>();
        report.put("success", successCount);
        report.put("skipped", skipCount);
        report.put("totalProcessed", students.size());

        return report;
    }

    // --- Helper Method ---
    private StudentDto mapToDto(Student student) {
        return StudentDto.builder()
                .id(student.getId())
                .userId(student.getUser() != null ? student.getUser().getId() : null)
                .userEmail(student.getUser() != null ? student.getUser().getEmail() : null)
                .studentCode(student.getStudentCode())
                .fullName(student.getFullName())
                .parentName(student.getParentName())
                .parentPhone(student.getParentPhone())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .phone(student.getPhone())
                .identityNumber(student.getIdentityNumber())
                .targetScore(student.getTargetScore())
                .status(student.getStatus())
                .createdAt(student.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StudentDto getMyProfile(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản của bạn chưa được liên kết với hồ sơ học sinh nào."));

        StudentDto dto = mapToDto(student);

        // Fetch current class
        List<com.lms.education.module.enrollment.entity.Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        if (enrollments != null && !enrollments.isEmpty()) {
            dto.setCurrentClassId(enrollments.get(0).getClasses().getId());
        }

        return dto;
    }
}