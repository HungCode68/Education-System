package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, String> {

    // Kiểm tra trùng tên
    boolean existsByName(String name);

    // Lấy danh sách theo trạng thái (Dùng cho các Dropdown list chọn môn học)
    List<Subject> findByIsActive(Boolean isActive);

    // Tìm kiếm & Phân trang (Dùng cho trang Quản lý môn học của Admin)
    @Query("SELECT s FROM Subject s WHERE " +
            "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:isActive IS NULL OR s.isActive = :isActive)")
    Page<Subject> search(String keyword, Boolean isActive, Pageable pageable);

    // Tìm chi tiết theo ID nhưng bắt buộc phải Active
    // Dùng cho phía Client/Học sinh (không cho xem môn đã bị khóa)
    Optional<Subject> findByIdAndIsActive(String id, Boolean isActive);
}
