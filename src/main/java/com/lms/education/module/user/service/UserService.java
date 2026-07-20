package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.User;
import java.time.Instant;
import java.util.Optional;

public interface UserService {
    // Tìm kiếm bằng email
    Optional<User> findByEmail(String email);

    // Quản lý Refresh Token
    void updateRefreshToken(Long userId, String refreshToken, Instant expiryDate);

    Optional<User> findByRefreshToken(String token);

    // Xóa token khi Logout
    void deleteRefreshToken(String token);

    // Tạo mới tài khoản
    UserDto createUser(UserDto userDto);

    // Cập nhật trạng thái tài khoản (ACTIVE, INACTIVE, LOCKED)
    void updateUserStatus(Long userId, String status);

    UserDto getUserById(Long id);
}