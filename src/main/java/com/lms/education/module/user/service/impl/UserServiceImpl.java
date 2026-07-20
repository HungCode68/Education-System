package com.lms.education.module.user.service.impl;

import com.lms.education.annotation.LogActivity;
import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.User;
import com.lms.education.module.user.repository.UserRepository;
import com.lms.education.module.user.service.UserService;
import com.lms.education.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
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

    // Helper method để map Entity sang DTO
    private UserDto mapToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .build();
    }
}