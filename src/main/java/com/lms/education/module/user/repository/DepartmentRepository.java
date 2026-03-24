package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    // Kiểm tra xem tên phòng ban đã tồn tại chưa (Dùng khi tạo mới/cập nhật để tránh trùng lặp)
    boolean existsByNameIgnoreCase(String name);

    // Tìm kiếm và Lọc danh sách phòng ban
    @Query("SELECT d FROM Department d WHERE " +
            "(:keyword IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:type IS NULL OR d.type = :type) " +
            "AND (:isActive IS NULL OR d.isActive = :isActive)")
    Page<Department> searchAndFilter(
            @Param("keyword") String keyword,
            @Param("type") Department.DepartmentType type,
            @Param("isActive") Boolean isActive,
            Pageable pageable);

    // Lấy danh sách tất cả các phòng ban đang hoạt động (Dùng để hiển thị lên Dropdown khi thêm mới Giáo viên)
    List<Department> findByIsActiveTrueOrderByNameAsc();

    // Lấy danh sách phòng ban đang hoạt động theo phân loại (VD: Chỉ lấy các Tổ bộ môn 'academic' để phân công giảng dạy)
    List<Department> findByTypeAndIsActiveTrueOrderByNameAsc(Department.DepartmentType type);
}
