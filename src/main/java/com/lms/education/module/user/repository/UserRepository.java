package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);
    // Kiểm tra xem có người dùng nào đang sở hữu Role này không
    boolean existsByRoleId(String roleId);

    // Tìm kiếm và lọc tài khoản
    @Query("SELECT u FROM User u WHERE " +
            "(:keyword IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:status IS NULL OR u.status = :status) " +
            "AND (:roleCode IS NULL OR u.role.code = :roleCode)")
    Page<User> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("status") User.UserStatus status,
            @Param("roleCode") String roleCode,
            Pageable pageable);
}
