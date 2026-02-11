package com.lms.education.module.academic.repository;

import com.lms.education.module.academic.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, String> {

    // ==========================================
    // KIỂM TRA RÀNG BUỘC (VALIDATION)
    // ==========================================

    // Kiểm tra trùng Tên trong cùng năm học (VD: Không thể có 2 cái "Học kỳ 1" trong năm 2025)
    boolean existsByNameAndSchoolYearId(String name, String schoolYearId);

    // Kiểm tra trùng Mã (Code)
    boolean existsByCodeAndSchoolYearId(String code, String schoolYearId);

    // Kiểm tra trùng Thứ tự (Priority)
    // Đảm bảo không có 2 học kỳ cùng là số 1 trong cùng năm.
    boolean existsByPriorityAndSchoolYearId(Integer priority, String schoolYearId);

    // ==========================================
    // TRUY VẤN DỮ LIỆU (DATA RETRIEVAL)
    // ==========================================

    // Lấy danh sách học kỳ của một Năm học
    // Sắp xếp theo Priority (1->2->3) để hiển thị đúng trình tự thời gian
    List<Semester> findBySchoolYearIdOrderByPriorityAsc(String schoolYearId);

    // Tìm học kỳ đang Active (Thường chỉ có 1 cái active trên toàn hệ thống hoặc theo năm)
    Optional<Semester> findFirstByStatus(Semester.SemesterStatus status);

    // Tìm học kỳ theo năm và thứ tự (VD: Lấy Học kỳ 1 của năm 2025)
    Optional<Semester> findBySchoolYearIdAndPriority(String schoolYearId, Integer priority);

    // ==========================================
    // LOGIC NÂNG CAO (COMPLEX LOGIC)
    // ==========================================

    // Kiểm tra chồng chéo thời gian (Overlap Dates)
    // Logic: Một năm học không thể có HK2 bắt đầu trước khi HK1 kết thúc.
    @Query("SELECT COUNT(s) > 0 FROM Semester s " +
            "WHERE s.schoolYear.id = :schoolYearId " +
            "AND (:excludeId IS NULL OR s.id != :excludeId) " + // Dùng khi Update (loại trừ chính nó)
            "AND (" +
            "   (:startDate BETWEEN s.startDate AND s.endDate) " +
            "   OR (:endDate BETWEEN s.startDate AND s.endDate) " +
            "   OR (s.startDate BETWEEN :startDate AND :endDate)" +
            ")")
    boolean existsOverlapDates(
            @Param("schoolYearId") String schoolYearId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") String excludeId
    );
}
