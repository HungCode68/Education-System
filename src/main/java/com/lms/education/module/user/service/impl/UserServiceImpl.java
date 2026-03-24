package com.lms.education.module.user.service.impl;

import com.lms.education.exception.ResourceNotFoundException;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.Teacher;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.TeacherRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changeUserRole(String userId, String roleCode) {
        // Tìm User
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản (User ID: " + userId + ")"));

        // Tìm Role mới trong Database
        Role newRole = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền có mã: " + roleCode));

        // Cập nhật Role cho User
        user.setRole(newRole);
        userRepository.save(user);

        log.info("Đã thay đổi quyền của User {} thành {}", user.getEmail(), roleCode);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String keyword, User.UserStatus status, String roleCode, Pageable pageable) {
        return userRepository.searchAndFilter(keyword, status, roleCode, pageable)
                .map(this::mapToDto);
    }

    @Override
    @Transactional
    public void updateUserStatus(String id, User.UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resetPassword(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với ID: " + userId));

        String defaultPassword = null;

        // Thử tìm xem tài khoản này có phải của Học sinh không
        Optional<Student> studentOpt = studentRepository.findByUserId(userId);
        if (studentOpt.isPresent()) {
            defaultPassword = studentOpt.get().getStudentCode(); // Lấy mã HS làm mật khẩu
        } else {
            // Nếu không phải HS, thử tìm xem có phải của Giáo viên không
            Optional<Teacher> teacherOpt = teacherRepository.findByUserId(userId);
            if (teacherOpt.isPresent()) {
                defaultPassword = teacherOpt.get().getTeacherCode(); // Lấy mã GV làm mật khẩu
            }
        }

        // Nếu tìm thấy mã (HS hoặc GV), tiến hành mã hóa và lưu lại
        if (defaultPassword != null) {
            user.setPassword(passwordEncoder.encode(defaultPassword));
            userRepository.save(user);
            log.info("Đã reset mật khẩu của tài khoản ID: {} về mã mặc định ({})", userId, defaultPassword);
        } else {
            throw new RuntimeException("Không thể reset: Tài khoản này không được liên kết với hồ sơ Học sinh hay Giáo viên nào.");
        }
    }

    // Hàm Helper để map Entity sang DTO
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus())
                .roleId(user.getRole() != null ? user.getRole().getId() : null)
                .roleCode(user.getRole() != null ? user.getRole().getCode() : null)
                .roleName(user.getRole() != null ? user.getRole().getName() : null)
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .build();
    }
}
