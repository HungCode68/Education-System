package com.lms.education.module.user.service;

import com.lms.education.module.user.dto.UserDto;
import com.lms.education.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    void changeUserRole(String userId, String roleCode);
    // CÁC HÀM QUẢN LÝ
    UserDto getUserById(String id);

    Page<UserDto> getAllUsers(String keyword, User.UserStatus status, String roleCode, Pageable pageable);

    void updateUserStatus(String id, User.UserStatus status);

    // hàm reset mật khẩu
    void resetPassword(String userId);
}
