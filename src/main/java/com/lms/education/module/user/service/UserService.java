package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
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

    // Xóa/Thu hồi tài khoản
    void deleteUser(Long id);

    // Lấy danh sách tài khoản có hỗ trợ tìm kiếm từ khóa (email/fullName) và phân trang
    Page<UserDto> getAllUsers(String keyword, Pageable pageable);

    // Cập nhật thông tin cơ bản của tài khoản (họ tên) và gán lại danh sách role theo tên (VD: ROLE_ADMIN)
    UserDto updateUser(Long id, String fullName, List<String> roleNames);

    // Gán lại riêng danh sách role cho tài khoản, không đổi các field khác
    UserDto updateUserRoles(Long id, List<String> roleNames);

    // Đặt lại mật khẩu tài khoản (dùng khi admin reset hộ người dùng quên mật khẩu)
    void resetPassword(Long id, String newPassword);
}