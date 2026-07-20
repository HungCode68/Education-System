package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // Kiểm tra tên quyền đã tồn tại chưa
    boolean existsByName(String name);

    // Tìm một Quyền cụ thể theo tên
    Optional<Permission> findByName(String name);

    // Tìm kiếm Quyền theo tên (cho ô Search)
    Page<Permission> findByNameContainingIgnoreCase(String name, Pageable pageable);
}