package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Tìm Role dựa theo tên (Ví dụ: ROLE_ADMIN)
    Optional<Role> findByName(String name);

    // Kiểm tra tên vai trò đã tồn tại chưa
    boolean existsByName(String name);

    // Tìm kiếm Role theo tên (Hỗ trợ thanh Search trên giao diện)
    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Kiểm tra xem có Role nào đang được gán Permission này không
    @Query("SELECT COUNT(r) > 0 FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    boolean isPermissionAssigned(Long permissionId);
}