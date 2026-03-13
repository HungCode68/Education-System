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
        if (teacherRepository.existsByTeacherCode(dto.getTeacherCode())) {
            throw new DuplicateResourceException("Mã giáo viên đã tồn tại");
        }

        Department department = null;
        if (dto.getDepartmentId() != null) {
            department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Phòng ban không tồn tại"));
        }

        // LOGIC MỚI: User có thể là null
        User savedUser = null;

        // Chỉ tạo tài khoản nếu có nhập Email liên hệ
        if (dto.getEmailContact() != null && !dto.getEmailContact().isBlank()) {
            if (userRepository.existsByEmail(dto.getEmailContact())) {
                throw new DuplicateResourceException("Email đã tồn tại: " + dto.getEmailContact());
            }
            User newUser = new User();
            newUser.setEmail(dto.getEmailContact());
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
    public Page<TeacherDto> getAll(Pageable pageable) {
        return teacherRepository.findAll(pageable).map(this::mapToDto);
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
