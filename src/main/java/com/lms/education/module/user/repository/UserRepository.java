package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Tìm kiếm người dùng bằng email
    Optional<User> findByEmail(String email);

    // Kiểm tra sự tồn tại của email trong hệ thống
    Boolean existsByEmail(String email);

    // Tìm kiếm người dùng dựa trên Refresh Token
    // Phục vụ cho logic gia hạn Access Token khi hết hạn
    Optional<User> findByRefreshToken(String refreshToken);
}