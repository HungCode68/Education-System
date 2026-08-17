package com.lms.education.module.user.repository;

import com.lms.education.module.user.entity.Staff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    // Kiểm tra trùng lặp mã nhân sự
    boolean existsByStaffCode(String staffCode);

    // Kiểm tra xem User này đã có hồ sơ nhân sự chưa (Quan hệ 1-1)
    boolean existsByUserId(Long userId);

    // Lấy danh sách giảng viên (Dựa trên phân loại hệ thống staffType)
    @Query("SELECT s FROM Staff s WHERE UPPER(s.staffType) = 'TEACHER'")
    List<Staff> findTeachers();

    // Tìm kiếm chính xác theo mã nhân sự
    Optional<Staff> findByStaffCode(String staffCode);

    // Tìm kiếm hồ sơ nhân sự dựa theo User ID
    Optional<Staff> findByUserId(Long userId);

    // Đếm số lượng nhân sự có mã bắt đầu bằng tiền tố cụ thể (Ví dụ: 'GV26%')
    @Query("SELECT COUNT(s) FROM Staff s WHERE s.staffCode LIKE CONCAT(:prefix, :year, '%')")
    long countByStaffCodePattern(@Param("prefix") String prefix, @Param("year") String year);

    // Hỗ trợ ô Search tổng hợp: Tìm theo Tên, Mã nhân viên hoặc Số điện thoại
    @Query("SELECT s FROM Staff s WHERE " +
            "LOWER(s.staffCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "s.phone LIKE CONCAT('%', :keyword, '%')")
    Page<Staff> searchStaffs(@Param("keyword") String keyword, Pageable pageable);

    // Thêm hàm lấy danh sách nhân sự theo phòng ban
    Page<Staff> findByDepartmentId(Long departmentId, Pageable pageable);

    // Kiểm tra xem có nhân sự nào đang trực thuộc phòng ban này không
    boolean existsByDepartmentId(Long departmentId);

    // Lọc danh sách nhân sự có staffType chứa từ khóa (Ví dụ: 'TEACHER')
    @Query("SELECT s FROM Staff s WHERE UPPER(s.staffType) LIKE UPPER(CONCAT('%', :typeKey, '%'))")
    List<Staff> findByStaffTypeContainingIgnoreCase(@Param("typeKey") String typeKey);
}