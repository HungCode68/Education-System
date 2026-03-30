package com.lms.education.module.user.service.impl;

import com.lms.education.exception.DuplicateResourceException;
import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.TeacherDto;
import com.lms.education.module.user.entity.Department;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.DepartmentRepository;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.service.TeacherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public TeacherDto create(TeacherDto dto) {
        // LOGIC TỰ ĐỘNG SINH MÃ GIÁO VIÊN (VD: GV26001)
        String currentYear = String.valueOf(java.time.Year.now().getValue()).substring(2);
        String prefix = "GV" + currentYear;

        String maxCode = teacherRepository.findMaxTeacherCodeByPrefix(prefix);
        String newTeacherCode;

        if (maxCode == null) {
            newTeacherCode = prefix + "001";
        } else {
            int nextSeq = Integer.parseInt(maxCode.substring(4)) + 1;
            newTeacherCode = prefix + String.format("%03d", nextSeq);
        }

        dto.setTeacherCode(newTeacherCode);
        log.info("Hệ thống tự động sinh mã giáo viên mới: {}", newTeacherCode);

        // TẠO TÀI KHOẢN & LƯU HỒ SƠ
        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban không tồn tại"));
        }

        User savedUser = null;

        if (dto.getEmailContact() != null && !dto.getEmailContact().isBlank()) {
            if (userRepository.existsByEmail(dto.getEmailContact())) {
                throw new DuplicateResourceException("Email đã tồn tại: " + dto.getEmailContact());
            }
            User newUser = new User();
            newUser.setEmail(dto.getEmailContact());
            // Mật khẩu lấy theo mã GV vừa sinh
            newUser.setPassword(passwordEncoder.encode(dto.getTeacherCode()));

            Role teacherRole = roleRepository.findByCode("SUBJECT_TEACHER")
                    .orElseThrow(() -> new RuntimeException("Lỗi: DB thiếu role SUBJECT_TEACHER"));

            newUser.setRole(teacherRole);
            savedUser = userRepository.save(newUser);
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
                .user(savedUser)
                .build();

        return mapToDto(teacherRepository.save(teacher));
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
    @Transactional(readOnly = true)
    public TeacherDto getByUserId(String userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ giáo viên không tồn tại!"));
        return mapToDto(teacher);
    }

    @Override
    public Page<TeacherDto> getAll(String keyword, Teacher.Status status, String departmentId, Pageable pageable) {
        return teacherRepository.searchAndFilter(keyword, status, departmentId, pageable)
                .map(this::mapToDto);
    }


    @Override
    @Transactional
    public void createAccountForExistingTeacher(String teacherId, String email) {
        // Tìm hồ sơ giáo viên
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        if (teacher.getUser() != null) {
            throw new DuplicateResourceException("Giáo viên này đã có tài khoản rồi!");
        }

        // XỬ LÝ EMAIL
        String finalEmail;

        if (email != null && !email.isBlank()) {
            finalEmail = email;
        } else {
            // TỰ ĐỘNG: Lấy Mã GV viết thường + đuôi email
            // Ví dụ: TeacherCode="GV001" -> Email="gv001@school.edu.vn"
            finalEmail = teacher.getTeacherCode().toLowerCase() + "@school.edu.vn";
        }

        if (userRepository.existsByEmail(finalEmail)) {
            throw new DuplicateResourceException("Email " + finalEmail + " đã tồn tại!");
        }

        // Tạo User
        User newUser = new User();
        newUser.setEmail(finalEmail);
        newUser.setPassword(passwordEncoder.encode(teacher.getTeacherCode()));

        com.lms.education.module.user.entity.Role teacherRole = roleRepository.findByCode("SUBJECT_TEACHER")
                .orElseThrow(() -> new RuntimeException("Lỗi: DB thiếu role SUBJECT_TEACHER"));
        newUser.setRole(teacherRole);

        User savedUser = userRepository.save(newUser);

        // Update Teacher
        teacher.setUser(savedUser);
        teacher.setEmailContact(finalEmail);
        teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public java.util.Map<String, Object> createAccountsBatch(java.util.List<String> teacherIds) {
        int successCount = 0;
        int failCount = 0;
        java.util.List<String> failedDetails = new java.util.ArrayList<>();

        // Query lấy Role đúng 1 lần duy nhất để gán cho tất cả
        com.lms.education.module.user.entity.Role teacherRole = roleRepository.findByCode("SUBJECT_TEACHER")
                .orElseThrow(() -> new RuntimeException("Lỗi cấu hình: Chưa có role SUBJECT_TEACHER trong Database"));

        // Lấy toàn bộ danh sách giáo viên cần tạo bằng 1 câu query
        java.util.List<Teacher> teachers = teacherRepository.findAllById(teacherIds);

        for (Teacher teacher : teachers) {
            try {
                // Kiểm tra Đã có tài khoản chưa?
                if (teacher.getUser() != null) {
                    failCount++;
                    failedDetails.add("Giáo viên " + teacher.getTeacherCode() + " đã có tài khoản.");
                    continue; // Bỏ qua, chạy sang giáo viên tiếp theo
                }

                // Tạo email mặc định dựa trên mã giáo viên (VD: gv26001@school.edu.vn)
                String finalEmail = teacher.getTeacherCode().toLowerCase() + "@school.edu.vn";

                // Kiểm tra Trùng email?
                if (userRepository.existsByEmail(finalEmail)) {
                    failCount++;
                    failedDetails.add("Giáo viên " + teacher.getTeacherCode() + " - Email " + finalEmail + " đã bị trùng.");
                    continue;
                }

                // Khởi tạo User mới
                User newUser = new User();
                newUser.setEmail(finalEmail);
                // Mật khẩu mặc định là mã giáo viên
                newUser.setPassword(passwordEncoder.encode(teacher.getTeacherCode()));
                newUser.setRole(teacherRole);

                User savedUser = userRepository.save(newUser);

                // Cập nhật lại vào hồ sơ giáo viên
                teacher.setUser(savedUser);
                teacher.setEmailContact(finalEmail); // Cập nhật luôn email liên hệ bằng email đăng nhập
                successCount++;

            } catch (Exception e) {
                failCount++;
                failedDetails.add("Lỗi không xác định với giáo viên " + teacher.getTeacherCode() + ": " + e.getMessage());
                log.error("Lỗi khi tạo tài khoản hàng loạt cho giáo viên {}", teacher.getTeacherCode(), e);
            }
        }

        // Lưu toàn bộ danh sách giáo viên đã được cập nhật
        teacherRepository.saveAll(teachers);

        // Đóng gói kết quả trả về cho Frontend
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalProcessed", teacherIds.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedDetails", failedDetails);

        return result;
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
                .userId(teacher.getUser() != null ? teacher.getUser().getId() : null)
                .build();
    }
}
