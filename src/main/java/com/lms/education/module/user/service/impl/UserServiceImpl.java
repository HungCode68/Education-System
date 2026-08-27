package com.lms.education.module.user.service.impl;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.Role;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.RoleRepository;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.repository.StudentRepository;
import com.lms.education.module.user.repository.StaffRepository;
import com.lms.education.module.user.entity.Student;
import com.lms.education.module.user.entity.Staff;
import com.lms.education.module.user.service.UserService;
import com.lms.education.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public void updateRefreshToken(Long userId, String refreshToken, Instant expiryDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        user.setRefreshToken(refreshToken);
        user.setExpiryDate(expiryDate);
        userRepository.save(user);
    }

    @Override
    public Optional<User> findByRefreshToken(String token) {
        return userRepository.findByRefreshToken(token);
    }

    @Override
    @Transactional
    public void deleteRefreshToken(String token) {
        userRepository.findByRefreshToken(token).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setExpiryDate(null);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = User.builder()
                .email(userDto.getEmail())
                .password(passwordEncoder.encode("123456")) // Mật khẩu mặc định khi trung tâm cấp tài khoản
                .fullName(userDto.getFullName())
                .status("ACTIVE")
                .build();

        if (userDto.getRoles() != null) {
            user.setRoles(resolveRoles(new java.util.ArrayList<>(userDto.getRoles())));
        } else {
            user.setRoles(new HashSet<>());
        }

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }


    @Override
    @Transactional
    public void updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        user.setStatus(status);
        userRepository.save(user);

        // Nếu khóa tài khoản, xóa luôn Refresh Token để buộc logout
        if ("LOCKED".equals(status) || "INACTIVE".equals(status)) {
            user.setRefreshToken(null);
            user.setExpiryDate(null);
        }
    }

    @Override
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(String keyword, Pageable pageable) {
        Page<User> users;

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            users = userRepository.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(kw, kw, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        return users.map(this::mapToDto);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, String fullName, List<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }

        if (roleNames != null) {
            user.setRoles(resolveRoles(roleNames));
        }

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUserRoles(Long id, List<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        user.setRoles(resolveRoles(roleNames));

        User savedUser = userRepository.save(user);
        return mapToDto(savedUser);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        String finalPassword = "123456";
        Optional<Student> studentOpt = studentRepository.findByUserId(id);
        if (studentOpt.isPresent()) {
            finalPassword = studentOpt.get().getStudentCode().toUpperCase();
        } else {
            Optional<Staff> staffOpt = staffRepository.findByUserId(id);
            if (staffOpt.isPresent()) {
                finalPassword = staffOpt.get().getStaffCode().toUpperCase();
            }
        }

        user.setPassword(passwordEncoder.encode(finalPassword));
        // Xóa refresh token hiện tại để buộc đăng nhập lại bằng mật khẩu mới trên mọi thiết bị
        user.setRefreshToken(null);
        user.setExpiryDate(null);

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        String currentUserEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng hiện tại"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Mật khẩu cũ không chính xác");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        // Xóa refresh token để các phiên đăng nhập khác bị đăng xuất
        user.setRefreshToken(null);
        user.setExpiryDate(null);
        userRepository.save(user);
    }

    // Chuyển danh sách tên role (VD: "ROLE_ADMIN") thành Set<Role> entity thật, ném lỗi rõ ràng nếu tên không tồn tại
    private Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return new HashSet<>();
        }

        return roleNames.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với tên: " + name)))
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));

        // Hủy liên kết với Học viên (nếu có)
        Optional<Student> studentOpt = studentRepository.findByUserId(id);
        studentOpt.ifPresent(student -> {
            student.setUser(null);
            studentRepository.save(student);
        });

        // Hủy liên kết với Nhân sự (nếu có)
        Optional<Staff> staffOpt = staffRepository.findByUserId(id);
        staffOpt.ifPresent(staff -> {
            staff.setUser(null);
            staffRepository.save(staff);
        });

        // Xóa Refresh Token (nếu có)
        user.setRefreshToken(null);
        user.setExpiryDate(null);
        userRepository.save(user);

        // Xóa User khỏi DB (JPA tự động xóa bản ghi trong bảng user_roles)
        userRepository.delete(user);
    }

    // Helper method để map Entity sang DTO
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(null) // TODO: entity User chưa có cột phone trong DB, tạm để null cho tới khi bổ sung migration
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}