package com.lms.education.module.teaching_assignment.repository;

import com.lms.education.module.teaching_assignment.entity.TeachingSubstitution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeachingSubstitutionRepository extends JpaRepository<TeachingSubstitution, String> {

    // TRA CỨU CƠ BẢN

    // Tìm các lượt dạy thay cho một phân công cụ thể
    // (VD: Xem lịch sử ai đã dạy thay cho lớp Toán 10A1 của cô Lan)
    List<TeachingSubstitution> findByOriginalAssignmentIdOrderByStartDateDesc(String assignmentId);

    // Tìm lịch dạy thay của một giáo viên (VD: Thầy Hùng tuần này dạy thay cho ai?)
    @Query("SELECT ts FROM TeachingSubstitution ts " +
            "WHERE ts.subTeacher.id = :teacherId " +
            "AND ts.status = 'approved' " +
            "ORDER BY ts.startDate DESC")
    List<TeachingSubstitution> findBySubTeacherId(@Param("teacherId") String teacherId);

    // =========================================
    // CHECK TRÙNG LỊCH (VALIDATION QUAN TRỌNG)

    // Check 1: "Xung đột Lớp học"
    // Trong khoảng thời gian này, Phân công này đã có ai dạy thay chưa?
    // -> Tránh việc lớp 10A1 vừa có thầy A dạy thay, vừa có thầy B dạy thay cùng ngày.
    @Query("SELECT COUNT(ts) > 0 FROM TeachingSubstitution ts " +
            "WHERE ts.originalAssignment.id = :assignmentId " +
            "AND ts.status != 'cancelled' " + // Không tính đơn đã hủy
            "AND ts.status != 'rejected' " +
            "AND (:excludeId IS NULL OR ts.id != :excludeId) " + // Dùng cho Update
            "AND (" +
            "   (:startDate BETWEEN ts.startDate AND ts.endDate) " +
            "   OR (:endDate BETWEEN ts.startDate AND ts.endDate) " +
            "   OR (ts.startDate BETWEEN :startDate AND :endDate)" +
            ")")
    boolean existsOverlapForAssignment(
            @Param("assignmentId") String assignmentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") String excludeId
    );

    // Check 2: "Xung đột Giáo viên"
    // Giáo viên dạy thay có bị kẹt lịch dạy thay khác không?
    // -> Thầy Hùng không thể dạy thay cho 2 lớp cùng 1 ngày.
    @Query("SELECT COUNT(ts) > 0 FROM TeachingSubstitution ts " +
            "WHERE ts.subTeacher.id = :subTeacherId " +
            "AND ts.status != 'cancelled' " +
            "AND ts.status != 'rejected' " +
            "AND (:excludeId IS NULL OR ts.id != :excludeId) " +
            "AND (" +
            "   (:startDate BETWEEN ts.startDate AND ts.endDate) " +
            "   OR (:endDate BETWEEN ts.startDate AND ts.endDate) " +
            "   OR (ts.startDate BETWEEN :startDate AND :endDate)" +
            ")")
    boolean existsOverlapForSubTeacher(
            @Param("subTeacherId") String subTeacherId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeId") String excludeId
    );

    // ==========================================
    // TÌM KIẾM NÂNG CAO

    // Tìm kiếm yêu cầu dạy thay, lọc theo Năm học, Học kỳ, Tên GV
    @Query("SELECT ts FROM TeachingSubstitution ts " +
            "JOIN FETCH ts.originalAssignment oa " +
            "JOIN FETCH oa.physicalClass pc " +
            "JOIN FETCH oa.subject s " +
            "JOIN FETCH oa.teacher mainTeacher " +
            "JOIN FETCH ts.subTeacher subTeacher " +
            "WHERE (:schoolYearId IS NULL OR oa.schoolYear.id = :schoolYearId) " +
            "AND (:semesterId IS NULL OR oa.semester.id = :semesterId) " +
            "AND (:keyword IS NULL OR " +
            "    LOWER(mainTeacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(subTeacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "    LOWER(pc.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY ts.startDate DESC")
    Page<TeachingSubstitution> search(
            @Param("schoolYearId") String schoolYearId,
            @Param("semesterId") String semesterId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // Tìm người đang dạy thay cho Assignment cụ thể vào NGÀY HÔM NAY
    @Query("SELECT ts FROM TeachingSubstitution ts " +
            "WHERE ts.originalAssignment.id = :assignmentId " +
            "AND :queryDate BETWEEN ts.startDate AND ts.endDate " + // Ngày query nằm trong khoảng dạy thay
            "AND ts.status = 'approved'") // Chỉ lấy đơn đã duyệt
    Optional<TeachingSubstitution> findActiveSubstitution(
            @Param("assignmentId") String assignmentId,
            @Param("queryDate") LocalDate queryDate
    );
}
