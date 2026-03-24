package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    // Tìm Role dựa theo mã code (Ví dụ: STUDENT)
    Optional<Role> findByCode(String code);

    // Kiểm tra mã vai trò đã tồn tại chưa (Dùng khi Thêm mới/Cập nhật để tránh lỗi UNIQUE của Database)
    boolean existsByCode(String code);

    // Lấy danh sách Role theo trạng thái (Dùng cho Frontend khi có bộ lọc Trạng thái: active/inactive)
    Page<Role> findByStatus(Role.RoleStatus status, Pageable pageable);

    // Tìm kiếm Role theo tên hoặc mã code (Hỗ trợ thanh Search trên giao diện quản lý Role)
    Page<Role> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);

    // Kiểm tra xem có Role nào đang được gán Permission này không
    @Query("SELECT COUNT(r) > 0 FROM Role r JOIN r.permissions p WHERE p.id = :permissionId")
    boolean isPermissionAssigned(@org.springframework.data.repository.query.Param("permissionId") Integer permissionId);
}
