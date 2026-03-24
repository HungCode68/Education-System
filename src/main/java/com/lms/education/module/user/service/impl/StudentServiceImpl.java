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

        // LOGIC TỰ ĐỘNG SINH MÃ HỌC SINH (VD: HS26001)
        // Lấy 2 số cuối của năm hiện tại (VD: Năm 2026 -> "26")
        String currentYear = String.valueOf(java.time.Year.now().getValue()).substring(2);
        String prefix = "HS" + currentYear;

        String maxCode = studentRepository.findMaxStudentCodeByPrefix(prefix);
        String newStudentCode;

        if (maxCode == null) {
            // Nếu chưa có học sinh nào trong năm nay
            newStudentCode = prefix + "001";
        } else {
            // maxCode có dạng "HS26005" -> Cắt bỏ 4 ký tự đầu ("HS26") để lấy "005", chuyển thành số rồi cộng 1
            int nextSeq = Integer.parseInt(maxCode.substring(4)) + 1;
            // Format lại thành chuỗi 3 chữ số (VD: 6 -> "006")
            newStudentCode = prefix + String.format("%03d", nextSeq);
        }

        // Gán mã vừa sinh vào DTO để lưu xuống DB và làm mật khẩu mặc định
        dto.setStudentCode(newStudentCode);
        log.info("Hệ thống tự động sinh mã học sinh mới: {}", newStudentCode);

        // CÁC LOGIC TẠO TÀI KHOẢN VÀ LƯU HỒ SƠ (Giữ nguyên như cũ)
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
            // Mật khẩu lúc này sẽ lấy đúng mã học sinh vừa được hệ thống tự sinh (VD: HS26001)
            newUser.setPassword(passwordEncoder.encode(dto.getStudentCode()));

            com.lms.education.module.user.entity.Role studentRole = roleRepository.findByCode("STUDENT")
                    .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: Chưa có role STUDENT trong Database"));

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
    public Page<StudentDto> getAll(String keyword, Student.Status status, Integer admissionYear, Pageable pageable) {
        // Truyền các tham số tìm kiếm xuống Repository
        return studentRepository.searchAndFilter(keyword, status, admissionYear, pageable)
                .map(this::mapToDto);
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
        // Mật khẩu mặc định vẫn để là Mã SV (viết hoa đúng như hồ sơ)
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


    @Override
    @Transactional
    public java.util.Map<String, Object> createAccountsBatch(java.util.List<String> studentIds) {
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedDetails = new java.util.ArrayList<>();

        // Query lấy Role đúng 1 lần duy nhất thay vì lấy n lần trong vòng lặp
        com.lms.education.module.user.entity.Role studentRole = roleRepository.findByCode("STUDENT")
                .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: Chưa có role STUDENT trong Database"));

        // Lấy toàn bộ danh sách học sinh cần tạo lên bằng 1 câu query
        java.util.List<Student> students = studentRepository.findAllById(studentIds);

        for (Student student : students) {
            try {
                // Kiểm tra Đã có tài khoản chưa?
                if (student.getUser() != null) {
                    failCount++;
                    failedDetails.add("Học sinh " + student.getStudentCode() + " đã có tài khoản.");
                    continue; // Bỏ qua, chạy sang em tiếp theo
                }

                // Tạo email mặc định
                String finalEmail = student.getStudentCode().toLowerCase() + "@school.edu.vn";

                // Kiểm tra Trùng email?
                if (userRepository.existsByEmail(finalEmail)) {
                    failCount++;
                    failedDetails.add("Học sinh " + student.getStudentCode() + " - Email " + finalEmail + " đã bị trùng.");
                    continue;
                }

                // Khởi tạo User mới
                User newUser = new User();
                newUser.setEmail(finalEmail);
                newUser.setPassword(passwordEncoder.encode(student.getStudentCode()));
                newUser.setRole(studentRole);

                User savedUser = userRepository.save(newUser);

                // Cập nhật lại vào hồ sơ học sinh
                student.setUser(savedUser);
                successCount++;

            } catch (Exception e) {
                failCount++;
                failedDetails.add("Lỗi không xác định với học sinh " + student.getStudentCode() + ": " + e.getMessage());
                log.error("Lỗi khi tạo tài khoản hàng loạt cho học sinh {}", student.getStudentCode(), e);
            }
        }

        // Lưu toàn bộ danh sách học sinh đã được cập nhật User
        studentRepository.saveAll(students);

        // Đóng gói kết quả trả về
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalProcessed", studentIds.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedDetails", failedDetails);

        return result;
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
