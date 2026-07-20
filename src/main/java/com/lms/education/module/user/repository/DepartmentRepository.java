package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    // Kiểm tra mã phòng ban đã tồn tại chưa (để validate UNIQUE)
    boolean existsByCode(String code);

    // Tìm một phòng ban cụ thể theo mã code
    Optional<Department> findByCode(String code);

    // Tìm kiếm phòng ban theo cả tên HOẶC mã code (Hỗ trợ ô Search tổng hợp trên giao diện)
    Page<Department> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}