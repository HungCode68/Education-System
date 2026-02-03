package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, String> {

    // Kiểm tra trùng tên (Ví dụ: Không được tạo 2 "Khối 10")
    boolean existsByName(String name);

    // Kiểm tra trùng Level (Quan trọng: Mỗi hệ thống chỉ nên có 1 level đại diện cho 1 khối)
    boolean existsByLevel(Integer level);

    // Tìm kiếm tên (Dùng cho logic Update để check trùng)
    Optional<Grade> findByName(String name);

    // Tìm kiếm theo Level (Dùng cho logic Update hoặc logic tự động lên lớp)
    Optional<Grade> findByLevel(Integer level);

    // Lấy danh sách Active (Dùng cho Dropdown chọn khối)
    List<Grade> findByIsActive(Boolean isActive, Sort sort);

    // Tìm kiếm & Phân trang (Admin dùng)
    @Query("SELECT g FROM Grade g WHERE " +
            "(:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:isActive IS NULL OR g.isActive = :isActive)")
    Page<Grade> search(String keyword, Boolean isActive, Pageable pageable);
}
