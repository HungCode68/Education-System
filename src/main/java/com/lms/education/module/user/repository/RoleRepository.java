package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    // Tìm Role dựa theo mã code (Ví dụ: STUDENT)
    Optional<Role> findByCode(String code);
}
