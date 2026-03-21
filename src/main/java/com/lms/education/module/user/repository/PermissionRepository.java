package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    // Kiểm tra mã quyền đã tồn tại chưa (Dùng để check Unique khi tạo mới/cập nhật)
    boolean existsByCode(String code);

    // Tìm một Quyền cụ thể theo mã
    Optional<Permission> findByCode(String code);

    // Lấy danh sách tất cả các Quyền thuộc một nhóm Phạm vi (Scope) cụ thể (Không phân trang, dùng để list ra Checkbox trên UI)
    List<Permission> findByScope(Permission.PermissionScope scope);

    // Lấy danh sách Quyền theo nhóm Phạm vi (Có phân trang cho bảng quản trị)
    Page<Permission> findByScope(Permission.PermissionScope scope, Pageable pageable);

    // Tìm kiếm Quyền theo tên hoặc mã code
    Page<Permission> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String name, String code, Pageable pageable);
}
