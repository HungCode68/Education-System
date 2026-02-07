package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.ClassTransferHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClassTransferHistoryRepository extends JpaRepository<ClassTransferHistory, String> {

    // Xem lịch sử của một học sinh cụ thể (Hiển thị trong Profile học sinh)
    // Sắp xếp ngày chuyển mới nhất lên đầu
    @Query("SELECT h FROM ClassTransferHistory h " +
            "JOIN FETCH h.fromClass fc " +
            "JOIN FETCH h.toClass tc " +
            "LEFT JOIN FETCH h.createdBy " + // User thực hiện có thể null
            "WHERE h.student.id = :studentId " +
            "ORDER BY h.transferDate DESC, h.createdAt DESC")
    List<ClassTransferHistory> findByStudentId(String studentId);

    // Tìm kiếm lịch sử chuyển lớp (Dùng cho trang Quản lý chuyển lớp của Admin)
    // Hỗ trợ lọc theo: Tên/Mã học sinh, Lớp đi/đến, Khoảng thời gian
    @Query("SELECT h FROM ClassTransferHistory h " +
            "JOIN FETCH h.student s " +
            "JOIN FETCH h.fromClass fc " +
            "JOIN FETCH h.toClass tc " +
            "WHERE " +
            // Tìm theo từ khóa (Tên HS hoặc Mã HS)
            "(:keyword IS NULL OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +

            // Lọc theo Lớp liên quan (Hoặc là lớp đi, hoặc là lớp đến đều hiển thị)
            "AND (:classId IS NULL OR fc.id = :classId OR tc.id = :classId) " +

            // Lọc theo khoảng thời gian (Từ ngày... Đến ngày...)
            "AND (:startDate IS NULL OR h.transferDate >= :startDate) " +
            "AND (:endDate IS NULL OR h.transferDate <= :endDate) " +

            "ORDER BY h.transferDate DESC, h.createdAt DESC")
    Page<ClassTransferHistory> search(String keyword, String classId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    // (Optional) Thống kê số lượng chuyển lớp trong một khoảng thời gian
    // Dùng để vẽ biểu đồ Dashboard (Ví dụ: Tháng 9 biến động nhiều nhất)
    @Query("SELECT COUNT(h) FROM ClassTransferHistory h " +
            "WHERE h.transferDate BETWEEN :startDate AND :endDate")
    long countTransfersBetween(LocalDate startDate, LocalDate endDate);
}
